if (!window.wsInitialized) {
    function initWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/lab1-1.0-SNAPSHOT/ws/marines`;
        
        window.ws = new WebSocket(wsUrl);
        
        window.ws.onopen = function() {
            console.log('WebSocket connected');
        };
        
        window.ws.onmessage = function(event) {
            console.log('WebSocket message:', event.data);
            if (window.handleWSMessage) {
                window.handleWSMessage(event.data);
            }
        };
        
        window.ws.onclose = function() {
            console.log('WebSocket disconnected, trying to reconnect...');
            setTimeout(initWebSocket, 3000);
        };
        
        window.ws.onerror = function(error) {
            // Игнорируем ошибки расширений браузера (например, React DevTools)
            if (error && error.message && error.message.includes('Receiving end does not exist')) {
                console.log('WebSocket extension error (can be ignored):', error.message);
                return;
            }
            console.error('WebSocket error:', error);
        };
    }

    window.globalWSHandlers = window.globalWSHandlers || [];
    window.handleWSMessage = window.handleWSMessage || function(message) {
        const [action, id] = message.split(':');
        
        if (window.globalWSHandlers) {
            window.globalWSHandlers.forEach(handler => {
                try {
                    handler(message, action, id);
                } catch (e) {
                    console.error('WebSocket handler error:', e);
                }
            });
        }
    };

    window.addWebSocketHandler = function(handler) {
        if (window.globalWSHandlers.indexOf(handler) === -1) {
            window.globalWSHandlers.push(handler);
        }
    };

    window.removeWebSocketHandler = function(handler) {
        const index = window.globalWSHandlers.indexOf(handler);
        if (index > -1) {
            window.globalWSHandlers.splice(index, 1);
        }
    };

    // Инициализируем WebSocket при загрузке DOM
    document.addEventListener("DOMContentLoaded", () => {
        if (!window.wsInitialized) {
            initWebSocket();
            window.wsInitialized = true;
        }
    });
}

// Глобальная функция для пагинации
window.paginationGoToPage = function(tableId, page) {
    let pageObject = null;
    if (tableId === 'chaptersTable') {
        pageObject = window.chaptersPage;
    } else if (tableId === 'coordinatesTable') {
        pageObject = window.coordsPage;
    }
    
    if (pageObject && pageObject.goToPage) {
        pageObject.goToPage(page);
    }
};

class CrudPage {
    constructor(config) {
        this.apiBase = config.apiBase;
        this.tableId = config.tableId;
        this.modalId = config.modalId;
        this.formId = config.formId;
        this.fields = config.fields; // {name: {type: "text|number|select", options: []?}}
        this.loadRelated = config.loadRelated;
        this.wsEvents = config.wsEvents || [];
        this.activeSorts = [];
        this.currentPage = 0;
        this.pageSize = 10;
        this.totalPages = 0;
        this.init();
    }

    init() {
        this.modal = document.getElementById(this.modalId);
        const closeBtn = this.modal.querySelector(".close");
        closeBtn.addEventListener("click", () => this.modal.style.display = "none");
        window.addEventListener("click", e => { if(e.target === this.modal) this.modal.style.display = "none"; });

        document.getElementById(this.formId).addEventListener("submit", e => this.save(e));
        if (this.loadRelated) this.loadRelated();

        this.load();
        
        this.initTableSorting();
        
        if (this.wsEvents.length > 0) {
            this.setupWebSocketListener();
        }
    }
    
    setupWebSocketListener() {
        this.wsHandler = (message, action, id) => {
            if (this.wsEvents.includes(action)) {
                console.log(`WebSocket event ${action} received for ${this.apiBase}`);
                this.load();
                if (this.loadRelated) {
                    this.loadRelated();
                }
            }
        };
        
        if (window.addWebSocketHandler && this.wsHandler) {
            window.addWebSocketHandler(this.wsHandler);
        }
    }

    async load() {
        try {
            const params = new URLSearchParams();
            params.append('page', this.currentPage.toString());
            params.append('size', this.pageSize.toString());
            
            if (this.activeSorts && this.activeSorts.length > 0) {
                const sort = this.activeSorts[0];
                params.append('sortBy', sort.field);
                params.append('sortOrder', sort.order);
            }
            
            const url = `${this.apiBase}?${params}`;
            
            const res = await fetch(url);
            const responseData = await res.json();
            
            if (responseData.content) {
                this.data = responseData.content;
                if (responseData.page) {
                    this.totalPages = responseData.page.totalPages || 0;
                    this.updatePaginationControls();
                }
            } else if (Array.isArray(responseData)) {
                this.data = responseData;
            } else {
                throw new Error("API вернул неверный формат данных");
            }
            
            if (!Array.isArray(this.data)) {
                throw new Error("API вернул неверный формат данных: ожидается массив");
            }
            
            let sortedData = this.data;

            const tbody = document.querySelector(`#${this.tableId} tbody`);
            tbody.innerHTML = "";
            sortedData.forEach(item => {
                const row = document.createElement("tr");
                row.innerHTML = this.renderRow(item);
                row.addEventListener("click", () => {
                    document.querySelectorAll(`#${this.tableId} tr`).forEach(r => r.classList.remove("selected"));
                    row.classList.add("selected");
                    this.selected = item;
                });
                tbody.appendChild(row);
            });
            
            // Обновляем иконки сортировки после загрузки
            if (this.updateSortIcons) {
                this.updateSortIcons();
            }
        } catch(e){ console.error(e); }
    }

    renderRow(item) {
        // To override in subclasses
        return "<td>Not implemented</td>";
    }

    async save(e) {
        e.preventDefault();
        const form = e.target;
        const payload = {};
        Object.keys(this.fields).forEach(f => {
            let value = form[f].value;
            if(this.fields[f].type === "number") {
                const numValue = parseFloat(value);
                payload[f] = isNaN(numValue) ? 0 : numValue;
            } else {
                payload[f] = value || "";
            }
        });
        
        const isUpdate = this.selected && this.selected.id;
        const method = isUpdate ? "PUT" : "POST";
        const url = isUpdate ? `${this.apiBase}/${this.selected.id}` : this.apiBase;
        
        try {
            const res = await fetch(url, {
                method: method,
                headers: {"Content-Type":"application/json"},
                body: JSON.stringify(payload)
            });
            if(!res.ok) throw new Error("Ошибка сохранения");
            await showAlert("✅ Сохранено", "Успех");
            this.modal.style.display = "none";
            this.selected = null; // Clear selection after save
            this.load();
        } catch(e){ await showAlert(e.toString(), "Ошибка"); }
    }

    // Методы для сортировки
    initTableSorting() {
        const sortableHeaders = document.querySelectorAll(`#${this.tableId} th.sortable`);
        
        sortableHeaders.forEach(header => {
            header.addEventListener('click', () => {
                const field = header.dataset.field;
                this.handleHeaderSort(field);
            });
        });
        
        this.updateSortIcons();
    }

    handleHeaderSort(field) {
        // Убираем множественную сортировку - только одна сортировка за раз
        
        if (this.activeSorts.length > 0 && this.activeSorts[0].field === field) {
            // То же поле - меняем порядок или убираем
            const currentOrder = this.activeSorts[0].order;
            if (currentOrder === 'asc') {
                this.activeSorts[0].order = 'desc';
            } else {
                // Убираем сортировку полностью
                this.activeSorts = [];
            }
        } else {
            // Новое поле - заменяем текущую сортировку
            this.activeSorts = [{ field: field, order: 'asc' }];
        }
        
        this.updateSortIcons();
        this.currentPage = 0; // Сбрасываем на первую страницу при сортировке
        this.load(); // Перезагружаем данные
    }

    updateSortIcons() {
        const sortableHeaders = document.querySelectorAll(`#${this.tableId} th.sortable`);
        
        sortableHeaders.forEach(header => {
            const field = header.dataset.field;
            const sortIcon = header.querySelector('.sort-icon');
            
            if (!sortIcon) return;
            
            // Сбрасываем все классы
            sortIcon.className = 'sort-icon';
            
            const activeSort = this.activeSorts.length > 0 && this.activeSorts[0].field === field ? this.activeSorts[0] : null;
            if (activeSort) {
                // Показываем конкретное направление сортировки
                sortIcon.classList.add(activeSort.order);
                sortIcon.removeAttribute('data-priority'); // Убираем номера приоритета
            } else {
                // Нет сортировки
                sortIcon.textContent = '↕';
                sortIcon.removeAttribute('data-priority');
            }
        });
    }

    updatePaginationControls() {
        const paginationDiv = document.getElementById("pagination");
        if (!paginationDiv) return;
        
        // Находим глобальный объект страницы по tableId
        let pageObject = null;
        if (this.tableId === 'chaptersTable') {
            pageObject = window.chaptersPage;
        } else if (this.tableId === 'coordinatesTable') {
            pageObject = window.coordsPage;
        }
        
        // Если объект не найден, не показываем пагинацию
        if (!pageObject) return;
        
        // Показываем кнопки только если есть больше одной страницы
        if (this.totalPages <= 1) {
            paginationDiv.innerHTML = `<span>Страница ${this.currentPage + 1} из ${this.totalPages}</span>`;
        } else {
            paginationDiv.innerHTML = `
                <button ${this.currentPage === 0 ? 'disabled' : ''} onclick="window.paginationGoToPage('${this.tableId}', 0)">Первая</button>
                <button ${this.currentPage === 0 ? 'disabled' : ''} onclick="window.paginationGoToPage('${this.tableId}', ${this.currentPage - 1})">Предыдущая</button>
                <span>Страница ${this.currentPage + 1} из ${this.totalPages}</span>
                <button ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''} onclick="window.paginationGoToPage('${this.tableId}', ${this.currentPage + 1})">Следующая</button>
                <button ${this.currentPage >= this.totalPages - 1 ? 'disabled' : ''} onclick="window.paginationGoToPage('${this.tableId}', ${this.totalPages - 1})">Последняя</button>
            `;
        }
    }

    goToPage(page) {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
            this.load();
        }
    }
}
