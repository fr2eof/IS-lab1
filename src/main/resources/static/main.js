const API_BASE = "/api/spacemarines";
const API_CHAPTERS = "/api/chapters";
const API_COORDS = "/api/coordinates";

let selectedMarine = null;
let ws = null;
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let currentFilter = "";
let currentSortBy = "";
let currentSortOrder = "asc";
let activeSorts = []; // Массив для множественной сортировки: [{field: "name", order: "asc"}, {field: "id", order: "desc"}]

// Инициализация WebSocket
function initWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/marines`;
    
    ws = new WebSocket(wsUrl);
    
    ws.onopen = function() {
        console.log('WebSocket connected');
    };
    
    ws.onmessage = function(event) {
        console.log('WebSocket message:', event.data);
        handleWebSocketMessage(event.data);
    };
    
    ws.onclose = function() {
        console.log('WebSocket disconnected, trying to reconnect...');
        setTimeout(initWebSocket, 3000); // Переподключение через 3 секунды
    };
    
    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
}

function handleWebSocketMessage(message) {
    const [action, id] = message.split(':');
    
    // Вызываем специфичные обработчики для страницы SpaceMarine
    switch(action) {
        case 'created':
        case 'updated':
        case 'deleted':
            if (typeof loadMarines === 'function') loadMarines(); // Перезагружаем таблицу SpaceMarine
            break;
        case 'chapter_created':
        case 'chapter_updated':  
        case 'chapter_deleted':
            if (typeof loadChaptersAndCoordinates === 'function') loadChaptersAndCoordinates();
            if (typeof loadMarines === 'function') loadMarines();
            break;
        case 'coordinates_created':
        case 'coordinates_updated':
        case 'coordinates_deleted':
            if (typeof loadChaptersAndCoordinates === 'function') loadChaptersAndCoordinates();
            if (typeof loadMarines === 'function') loadMarines();
            break;
    }
    
    // Вызываем глобальные обработчики
    if (window.globalWSHandlers) {
        window.globalWSHandlers.forEach(handler => {
            try {
                handler(message, action, id);
            } catch (e) {
                console.error('WebSocket handler error:', e);
            }
        });
    }
}

// Делаем обработчик глобальным
window.handleWSMessage = handleWebSocketMessage;
window.globalWS = true;

// Массив для хранения глобальных обработчиков WebSocket
window.globalWSHandlers = [];

// Функция для добавления обработчика WebSocket
window.addWebSocketHandler = function(handler) {
    if (window.globalWSHandlers.indexOf(handler) === -1) {
        window.globalWSHandlers.push(handler);
    }
};

// Функция для удаления обработчика WebSocket
window.removeWebSocketHandler = function(handler) {
    const index = window.globalWSHandlers.indexOf(handler);
    if (index > -1) {
        window.globalWSHandlers.splice(index, 1);
    }
};

document.addEventListener("DOMContentLoaded", async () => {
    initWebSocket(); // Инициализируем WebSocket
    
    await loadChaptersAndCoordinates();
    loadMarines();

    const modal = document.getElementById("marineModal");
    const closeBtn = modal.querySelector(".close");
    closeBtn.addEventListener("click", () => modal.style.display = "none");
    window.addEventListener("click", e => { if(e.target === modal) modal.style.display = "none"; });

    // Снятие выделения при клике вне таблицы
    document.addEventListener("click", (e) => {
        const table = document.getElementById("marineTable");
        const tableContainer = table ? table.closest(".container") : null;
        
        if (tableContainer && !tableContainer.contains(e.target)) {
            // Клик произошел вне таблицы
            document.querySelectorAll("#marineTable tr").forEach(r => r.classList.remove("selected"));
            selectedMarine = null;
        }
    });

    document.getElementById("createMarineBtn").addEventListener("click", () => {
        // Снимаем выделение со всех строк
        document.querySelectorAll("#marineTable tr").forEach(r => r.classList.remove("selected"));
        selectedMarine = null; // Режим создания
        document.getElementById("marineForm").reset();
        console.log("Режим создания: selectedMarine сброшен, форма очищена");
        modal.style.display = "block";
    });
    
    document.getElementById("updateMarineBtn").addEventListener("click", async () => {
        console.log("=== Кнопка Обновить нажата ===");
        console.log("selectedMarine:", selectedMarine);
        console.log("selectedMarine?.id:", selectedMarine?.id);
        
        // Проверяем, есть ли выделенная строка в таблице
        const selectedRow = document.querySelector("#marineTable tr.selected");
        console.log("selectedRow:", selectedRow);
        
        if (!selectedMarine || !selectedMarine.id) {
            // Пробуем получить данные из выделенной строки
            if (selectedRow && selectedRow.marineData) {
                console.log("Используем данные из выделенной строки");
                selectedMarine = selectedRow.marineData;
            } else {
                await showAlert("Выберите десантника для редактирования", "Предупреждение");
                return;
            }
        }
        
        // Убеждаемся, что строка выделена в таблице
        const rows = document.querySelectorAll("#marineTable tbody tr");
        let found = false;
        for (let row of rows) {
            const idCell = row.cells[0];
            if (idCell && idCell.textContent.trim() === selectedMarine.id.toString()) {
                document.querySelectorAll("#marineTable tr").forEach(r => r.classList.remove("selected"));
                row.classList.add("selected");
                found = true;
                // Обновляем selectedMarine из данных строки, если они есть
                if (row.marineData) {
                    selectedMarine = row.marineData;
                }
                break;
            }
        }
        if (!found) {
            await showAlert("Не удалось найти выбранного десантника в таблице", "Ошибка");
            return;
        }
        
        console.log("Режим обновления: selectedMarine.id =", selectedMarine.id);
        console.log("selectedMarine полный объект:", selectedMarine);
        fillFormForEdit(selectedMarine);
        modal.style.display = "block";
    });
    
    document.getElementById("deleteMarineBtn").addEventListener("click", deleteMarine);
    
    document.getElementById("marineForm").addEventListener("submit", saveMarine);

    // Обработчик изменения размера страницы
    document.getElementById("pageSizeSelect").addEventListener("change", (e) => {
        pageSize = parseInt(e.target.value);
        currentPage = 0; // Сбрасываем на первую страницу
        loadMarines();
    });


    // Инициализация сортировки по заголовкам
    initTableSorting();
});

// Функции для сортировки по заголовкам
function initTableSorting() {
    const sortableHeaders = document.querySelectorAll('#marineTable th.sortable');
    
    sortableHeaders.forEach(header => {
        header.addEventListener('click', () => {
            const field = header.dataset.field;
            handleHeaderSort(field);
        });
    });
    
    updateSortIcons();
}

function handleHeaderSort(field) {
    // Убираем множественную сортировку - только одна сортировка за раз
    
    if (activeSorts.length > 0 && activeSorts[0].field === field) {
        // То же поле - меняем порядок или убираем
        const currentOrder = activeSorts[0].order;
        if (currentOrder === 'asc') {
            activeSorts[0].order = 'desc';
            currentSortBy = field;
            currentSortOrder = 'desc';
        } else {
            // Убираем сортировку полностью
            activeSorts = [];
            currentSortBy = "";
            currentSortOrder = "asc";
        }
    } else {
        // Новое поле - заменяем текущую сортировку
        activeSorts = [{ field: field, order: 'asc' }];
        currentSortBy = field;
        currentSortOrder = 'asc';
    }
    
    updateSortIcons();
    currentPage = 0; // Сбрасываем на первую страницу при сортировке
    loadMarines();
}

function updateSortIcons() {
    const sortableHeaders = document.querySelectorAll('#marineTable th.sortable');
    
    sortableHeaders.forEach(header => {
        const field = header.dataset.field;
        const sortIcon = header.querySelector('.sort-icon');
        
        if (!sortIcon) return;
        
        // Сбрасываем все классы
        sortIcon.className = 'sort-icon';
        
        const activeSort = activeSorts.length > 0 && activeSorts[0].field === field ? activeSorts[0] : null;
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

async function loadChaptersAndCoordinates() {
    const chaptersSelect = document.querySelector("select[name='chapterId']");
    const coordsSelect = document.querySelector("select[name='coordinatesId']");

    try {
        const [chaptersRes, coordsRes] = await Promise.all([
            fetch(`${API_CHAPTERS}?page=0&size=1000`), 
            fetch(`${API_COORDS}?page=0&size=1000`)
        ]);
        
        if (!chaptersRes.ok) {
            const errorData = await chaptersRes.json().catch(() => ({ error: chaptersRes.statusText }));
            const errorMsg = errorData.error || errorData.message || `HTTP ${chaptersRes.status}`;
            console.error("Chapters API error:", errorData);
            throw new Error(`Ошибка загрузки chapters: ${errorMsg}`);
        }
        if (!coordsRes.ok) {
            const errorData = await coordsRes.json().catch(() => ({ error: coordsRes.statusText }));
            const errorMsg = errorData.error || errorData.message || `HTTP ${coordsRes.status}`;
            console.error("Coordinates API error:", errorData);
            throw new Error(`Ошибка загрузки coordinates: ${errorMsg}`);
        }
        
        const chaptersData = await chaptersRes.json();
        const coordsData = await coordsRes.json();

        // Обработка PageResponse или обычного массива
        const chapters = chaptersData.content || chaptersData;
        const coords = coordsData.content || coordsData;

        if (!Array.isArray(chapters)) {
            throw new Error("Chapters API вернул неверный формат данных");
        }
        if (!Array.isArray(coords)) {
            throw new Error("Coordinates API вернул неверный формат данных");
        }

        chaptersSelect.innerHTML = '<option value="">--Выберите орден (необязательно)--</option>' + chapters.map(c => `<option value="${c.id}">${c.name}</option>`).join("");
        coordsSelect.innerHTML = '<option value="">--Выберите координаты (обязательно)--</option>' + coords.map(c => `<option value="${c.id}">x:${c.x}, y:${c.y}</option>`).join("");
    } catch(e) { 
        console.error("Ошибка загрузки связей:", e);
        // Используем try-catch чтобы не блокировать загрузку страницы
        try {
            await showAlert("Ошибка загрузки связей: " + e, "Ошибка");
        } catch(alertError) {
            console.error("Не удалось показать alert:", alertError);
        }
    }
}

async function loadMarines() {
    try {
        const params = new URLSearchParams({
            page: currentPage.toString(),
            size: pageSize.toString()
        });
        
        if (currentFilter) {
            params.append('nameFilter', currentFilter);
        }
        // Используем сортировку (только одна активная)
        if (activeSorts && activeSorts.length > 0) {
            const sort = activeSorts[0];
            const backendSupportedFields = ['id', 'name', 'health', 'heartCount', 'category', 'weaponType'];
            
            if (backendSupportedFields.includes(sort.field)) {
                params.append('sortBy', sort.field);
                params.append('sortOrder', sort.order);
            }
        }
        
        const res = await fetch(`${API_BASE}?${params}`);
        
        if (!res.ok) {
            const errorData = await res.json().catch(() => ({ error: res.statusText }));
            const errorMsg = errorData.error || errorData.message || `HTTP ${res.status}`;
            console.error("SpaceMarines API error:", errorData);
            throw new Error(`Ошибка загрузки: ${errorMsg}`);
        }
        
        const data = await res.json();
        const marines = data.content || data;
        const tbody = document.querySelector("#marineTable tbody");
        tbody.innerHTML = "";

        if (!Array.isArray(marines)) {
            throw new Error("Неверный формат данных: ожидается массив");
        }

        // Обновляем информацию о пагинации
        if (typeof data.totalPages !== 'undefined') {
            totalPages = data.totalPages;
            updatePaginationControls();
        } else if (data.page && data.page.totalPages) {
            totalPages = data.page.totalPages;
            updatePaginationControls();
        }

        // Применяем клиентскую сортировку только для полей, не поддерживаемых backend
        let sortedMarines = marines;
        if (activeSorts && activeSorts.length > 0) {
            const sort = activeSorts[0];
            const backendSupportedFields = ['id', 'name', 'health', 'heartCount', 'category', 'weaponType'];
            
            // Клиентская сортировка только для coordinates и chapter
            if (!backendSupportedFields.includes(sort.field)) {
                sortedMarines = [...marines].sort((a, b) => {
                    let aVal = a[sort.field];
                    let bVal = b[sort.field];
                    
                    // Обработка вложенных объектов для coordinates и chapter
                    if (sort.field === 'coordinates') {
                        aVal = a.coordinates ? `${a.coordinates.x},${a.coordinates.y}` : null;
                        bVal = b.coordinates ? `${b.coordinates.x},${b.coordinates.y}` : null;
                    } else if (sort.field === 'chapter') {
                        aVal = a.chapter ? a.chapter.name : null;
                        bVal = b.chapter ? b.chapter.name : null;
                    }
                    
                    // Обработка null/undefined значений
                    if (aVal == null && bVal == null) return 0;
                    if (aVal == null) return sort.order === 'asc' ? 1 : -1;
                    if (bVal == null) return sort.order === 'asc' ? -1 : 1;
                    
                    // Сравнение значений
                    if (typeof aVal === 'string') {
                        aVal = aVal.toLowerCase();
                        bVal = bVal.toLowerCase();
                    }
                    
                    if (aVal < bVal) return sort.order === 'asc' ? -1 : 1;
                    if (aVal > bVal) return sort.order === 'asc' ? 1 : -1;
                    return 0;
                });
            }
        }

        sortedMarines.forEach(m => {
            // Убеждаемся, что координаты и chapter ID доступны напрямую
            if (!m.coordinatesId && m.coordinates?.id) {
                m.coordinatesId = m.coordinates.id;
            }
            if (m.chapter && !m.chapterId && m.chapter.id) {
                m.chapterId = m.chapter.id;
            }
            
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${m.id}</td>
                <td class="editable" data-field="name" data-original="${m.name}">${m.name}</td>
                <td class="editable" data-field="health" data-original="${m.health}">${m.health}</td>
                <td class="editable" data-field="heartCount" data-original="${m.heartCount}">${m.heartCount}</td>
                <td class="editable dropdown-field" data-field="category" data-original="${m.category ?? ''}">${m.category ?? "-"}</td>
                <td class="editable dropdown-field" data-field="weaponType" data-original="${m.weaponType ?? ''}">${m.weaponType ?? "-"}</td>
                <td class="editable dropdown-field" data-field="coordinatesId" data-original="${m.coordinatesId ?? m.coordinates?.id ?? ''}">${m.coordinates ? `x:${m.coordinates.x}, y:${m.coordinates.y}` : "-"}</td>
                <td class="editable dropdown-field" data-field="chapterId" data-original="${m.chapterId ?? m.chapter?.id ?? ''}">${m.chapter?.name ?? "-"}</td>
            `;
            
            // Сохраняем объект в строке для быстрого доступа
            row.marineData = m;
            
            // Обработка клика для выделения
            row.addEventListener("click", (e) => {
                if (e.target.classList.contains('editable')) return; // Не выделяем при клике на editable поле
                document.querySelectorAll("#marineTable tr").forEach(r => r.classList.remove("selected"));
                row.classList.add("selected");
                selectedMarine = m; // Сохраняем выбранного десантника
            });
            
            // Добавляем обработчики для inline editing
            setupInlineEditing(row);
            
            tbody.appendChild(row);
        });
        
        // Обновляем иконки сортировки после загрузки данных
        updateSortIcons();
    } catch(e) { 
        console.error("Ошибка загрузки:", e);
        // Используем try-catch чтобы не блокировать загрузку страницы
        try {
            await showAlert("Ошибка загрузки: " + e, "Ошибка");
        } catch(alertError) {
            console.error("Не удалось показать alert:", alertError);
        }
    }
}

function updatePaginationControls() {
    const paginationDiv = document.getElementById("pagination");
    if (!paginationDiv) return;
    
    // Показываем кнопки только если есть больше одной страницы
    if (totalPages <= 1) {
        paginationDiv.innerHTML = `<span>Страница ${currentPage + 1} из ${totalPages}</span>`;
    } else {
        paginationDiv.innerHTML = `
            <button ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(0)">Первая</button>
            <button ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">Предыдущая</button>
            <span>Страница ${currentPage + 1} из ${totalPages}</span>
            <button ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">Следующая</button>
            <button ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${totalPages - 1})">Последняя</button>
        `;
    }
}

function setupInlineEditing(row) {
    const editableCells = row.querySelectorAll('.editable');
    
    editableCells.forEach(cell => {
        cell.addEventListener('dblclick', (e) => {
            e.stopPropagation();
            startInlineEdit(cell, row.marineData);
        });
        
        // Добавляем визуальную подсказку что поле можно редактировать
        cell.style.cursor = 'pointer';
        cell.title = 'Двойной клик для редактирования';
    });
}

function startInlineEdit(cell, marineData) {
    const field = cell.dataset.field;
    const currentValue = cell.textContent.trim();
    const originalValue = cell.dataset.original;
    
    if (cell.classList.contains('dropdown-field')) {
        // Обработка dropdown полей
        if (field === 'coordinatesId' || field === 'chapterId') {
            // Для связанных объектов загружаем данные с сервера
            loadRelatedObjectsForEdit(cell, field, originalValue, marineData, currentValue);
        } else {
            // Для enum полей используем статический список
            const options = field === 'category' 
                ? ['', 'ASSAULT', 'SUPPRESSOR', 'TERMINATOR', 'CHAPLAIN']
                : ['', 'BOLT_PISTOL', 'COMBI_FLAMER', 'COMBI_PLASMA_GUN', 'FLAMER', 'MULTI_MELTA'];
                
            const select = document.createElement('select');
            select.style.width = '100%';
            select.style.border = 'none';
            select.style.background = 'transparent';
            
            options.forEach(option => {
                const optionElement = document.createElement('option');
                optionElement.value = option;
                optionElement.textContent = option === '' ? '--Выберите--' : option;
                optionElement.selected = option === originalValue;
                select.appendChild(optionElement);
            });
            
            cell.innerHTML = '';
            cell.appendChild(select);
            select.focus();
            
            let isProcessing = false; // Флаг для предотвращения двойного вызова
            
            const finishEdit = async () => {
                if (isProcessing) return; // Предотвращаем повторный вызов
                isProcessing = true;
                
                const newValue = select.value;
                if (newValue !== originalValue) {
                    await confirmInlineEdit(cell, field, newValue, originalValue, marineData);
                } else {
                    cancelInlineEdit(cell, originalValue);
                }
                isProcessing = false;
            };
            
            select.addEventListener('blur', () => {
                // Добавляем небольшую задержку, чтобы Enter успел обработаться
                setTimeout(() => {
                    if (!isProcessing && document.activeElement !== select) {
                        finishEdit();
                    }
                }, 100);
            });
            select.addEventListener('keydown', async (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault(); // Предотвращаем blur
                    await finishEdit();
                } else if (e.key === 'Escape') {
                    isProcessing = false;
                    cancelInlineEdit(cell, originalValue);
                }
            });
        }
    } else {
        // Обработка текстовых полей
        const input = document.createElement('input');
        input.type = field === 'name' ? 'text' : 'number';
        input.value = originalValue;
        input.style.width = '100%';
        input.style.border = 'none';
        input.style.background = 'transparent';
        
        if (field === 'health') {
            input.min = '1';
        } else if (field === 'heartCount') {
            input.min = '1';
            input.max = '3';
        }
        
            cell.innerHTML = '';
            cell.appendChild(input);
            input.focus();
            input.select();
            
            let isProcessing = false; // Флаг для предотвращения двойного вызова
            
            const finishEdit = async () => {
                if (isProcessing) return; // Предотвращаем повторный вызов
                isProcessing = true;
                
                const newValue = input.value.trim();
                if (newValue !== originalValue && newValue !== '') {
                    // Валидация
                    if (field === 'health' && (isNaN(newValue) || parseInt(newValue) < 1)) {
                        isProcessing = false;
                        await showAlert('Health должно быть числом больше 0', 'Ошибка валидации');
                        cancelInlineEdit(cell, originalValue);
                        return;
                    }
                    if (field === 'heartCount' && (isNaN(newValue) || parseInt(newValue) < 1 || parseInt(newValue) > 3)) {
                        isProcessing = false;
                        await showAlert('Heart Count должно быть от 1 до 3', 'Ошибка валидации');
                        cancelInlineEdit(cell, originalValue);
                        return;
                    }
                    if (field === 'name' && newValue === '') {
                        isProcessing = false;
                        await showAlert('Имя не может быть пустым', 'Ошибка валидации');
                        cancelInlineEdit(cell, originalValue);
                        return;
                    }
                    await confirmInlineEdit(cell, field, newValue, originalValue, marineData);
                    isProcessing = false;
                } else {
                    isProcessing = false;
                    cancelInlineEdit(cell, originalValue);
                }
            };
            
            input.addEventListener('blur', () => {
                // Добавляем небольшую задержку, чтобы Enter успел обработаться
                setTimeout(() => {
                    if (!isProcessing && document.activeElement !== input) {
                        finishEdit();
                    }
                }, 100);
            });
            input.addEventListener('keydown', async (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault(); // Предотвращаем blur
                    await finishEdit();
                } else if (e.key === 'Escape') {
                    isProcessing = false;
                    cancelInlineEdit(cell, originalValue);
                }
            });
    }
}

async function confirmInlineEdit(cell, field, newValue, originalValue, marineData) {
    let displayValue = newValue;
    
    // Для связанных объектов получаем правильное отображение
    if (field === 'coordinatesId' || field === 'chapterId') {
        try {
            let apiUrl;
            if (field === 'coordinatesId') {
                apiUrl = '/api/coordinates';
            } else {
                apiUrl = '/api/chapters';
            }
            
            const response = await fetch(apiUrl);
            const objects = await response.json();
            
            if (newValue) {
                const selectedObj = objects.find(obj => obj.id.toString() === newValue);
                if (selectedObj) {
                    if (field === 'coordinatesId') {
                        displayValue = `x:${selectedObj.x}, y:${selectedObj.y}`;
                    } else {
                        displayValue = selectedObj.name;
                    }
                }
            } else {
                displayValue = '-';
            }
        } catch (error) {
            console.error('Ошибка получения данных для отображения:', error);
            displayValue = newValue;
        }
    }
    
    const confirmMessage = field === 'coordinatesId' || field === 'chapterId' 
        ? `Изменить ${field === 'coordinatesId' ? 'координаты' : 'главу'} на "${displayValue}"?`
        : `Изменить ${field} с "${originalValue}" на "${newValue}"?`;
    
    const confirmed = await showConfirm(confirmMessage, 'Подтверждение изменения');
    if (confirmed) {
        // Обновляем данные
        const updatedData = { ...marineData };
        
        if (field === 'health' || field === 'heartCount') {
            updatedData[field] = parseInt(newValue);
        } else {
            updatedData[field] = newValue;
        }
        
        // Отправляем обновление на сервер
        await updateMarineInline(marineData.id, updatedData, cell, displayValue, newValue);
    } else {
        cancelInlineEdit(cell, originalValue);
    }
}

function cancelInlineEdit(cell, originalValue) {
    cell.innerHTML = originalValue || '-';
}

async function loadRelatedObjectsForEdit(cell, field, originalValue, marineData, currentDisplayValue) {
    try {
        // Показываем индикатор загрузки
        cell.innerHTML = '<span style="color: #666;">Загрузка...</span>';
        
        let apiUrl, labelField, valueField;
        
        if (field === 'coordinatesId') {
            apiUrl = '/api/coordinates';
            labelField = 'coordinates';
            valueField = 'id';
        } else if (field === 'chapterId') {
            apiUrl = '/api/chapters';
            labelField = 'name';
            valueField = 'id';
        }
        
        const response = await fetch(`${apiUrl}?page=0&size=1000`);
        if (!response.ok) {
            throw new Error('Не удалось загрузить данные');
        }
        
        const responseData = await response.json();
        const objects = responseData.content || responseData;
        
        // Проверяем, что получили массив
        if (!Array.isArray(objects)) {
            throw new Error('Неверный формат данных: ожидается массив');
        }
        
        // Создаем select элемент
        const select = document.createElement('select');
        select.style.width = '100%';
        select.style.border = 'none';
        select.style.background = 'transparent';
        
        // Добавляем опцию "Не выбрано" для необязательных полей
        if (field === 'chapterId') {
            const noneOption = document.createElement('option');
            noneOption.value = '';
            noneOption.textContent = '--Выберите главу--';
            noneOption.selected = originalValue === '';
            select.appendChild(noneOption);
        }
        
        // Добавляем опции из загруженных данных
        objects.forEach(obj => {
            const option = document.createElement('option');
            option.value = obj[valueField];
            
            let displayText;
            if (field === 'coordinatesId') {
                displayText = `x:${obj.x}, y:${obj.y}`;
            } else if (field === 'chapterId') {
                displayText = obj[labelField];
            }
            
            option.textContent = displayText;
            option.selected = obj[valueField].toString() === originalValue;
            select.appendChild(option);
        });
        
        cell.innerHTML = '';
        cell.appendChild(select);
        select.focus();
        
        let isProcessing = false; // Флаг для предотвращения двойного вызова
        
        const finishEdit = async () => {
            if (isProcessing) return; // Предотвращаем повторный вызов
            isProcessing = true;
            
            const newValue = select.value;
            if (newValue !== originalValue) {
                await confirmInlineEdit(cell, field, newValue, originalValue, marineData);
            } else {
                // Восстанавливаем отображение
                if (field === 'coordinatesId') {
                    const selectedObj = objects.find(obj => obj[valueField].toString() === newValue);
                    cell.innerHTML = selectedObj ? `x:${selectedObj.x}, y:${selectedObj.y}` : '-';
                } else if (field === 'chapterId') {
                    const selectedObj = objects.find(obj => obj[valueField].toString() === newValue);
                    cell.innerHTML = selectedObj ? selectedObj.name : '-';
                }
            }
            isProcessing = false;
        };
        
        select.addEventListener('blur', () => {
            // Добавляем небольшую задержку, чтобы Enter успел обработаться
            setTimeout(() => {
                if (!isProcessing && document.activeElement !== select) {
                    finishEdit();
                }
            }, 100);
        });
        select.addEventListener('keydown', async (e) => {
            if (e.key === 'Enter') {
                e.preventDefault(); // Предотвращаем blur
                await finishEdit();
            } else if (e.key === 'Escape') {
                isProcessing = false;
                // Восстанавливаем исходное значение
                if (field === 'coordinatesId') {
                    const selectedObj = objects.find(obj => obj[valueField].toString() === originalValue);
                    cell.innerHTML = selectedObj ? `x:${selectedObj.x}, y:${selectedObj.y}` : '-';
                } else if (field === 'chapterId') {
                    const selectedObj = objects.find(obj => obj[valueField].toString() === originalValue);
                    cell.innerHTML = selectedObj ? selectedObj.name : '-';
                }
            }
        });
        
    } catch (error) {
        console.error('Ошибка загрузки данных:', error);
        // Восстанавливаем исходное отображаемое значение
        cell.innerHTML = currentDisplayValue || '-';
        await showAlert('Ошибка загрузки данных для редактирования: ' + error.message, 'Ошибка');
    }
}

async function updateMarineInline(marineId, updatedData, cell, displayValue, actualValue) {
    try {
        // Сначала получаем полные данные десантника
        const getRes = await fetch(`${API_BASE}/${marineId}`);
        const fullMarineData = await getRes.json();
        
        // Убеждаемся, что ID доступны напрямую
        if (!fullMarineData.coordinatesId && fullMarineData.coordinates?.id) {
            fullMarineData.coordinatesId = fullMarineData.coordinates.id;
        }
        if (fullMarineData.chapter && !fullMarineData.chapterId && fullMarineData.chapter.id) {
            fullMarineData.chapterId = fullMarineData.chapter.id;
        }
        
        // Обновляем только нужные поля, используя новые значения из updatedData если они есть
        // Извлекаем ID из вложенных объектов или из updatedData
        const coordinatesId = updatedData.coordinatesId !== undefined 
            ? (updatedData.coordinatesId ? parseInt(updatedData.coordinatesId) : null)
            : (fullMarineData.coordinatesId || fullMarineData.coordinates?.id || null);
        
        const chapterId = updatedData.chapterId !== undefined 
            ? (updatedData.chapterId ? parseInt(updatedData.chapterId) : null)
            : (fullMarineData.chapterId || fullMarineData.chapter?.id || null);
        
        // Координаты обязательны
        if (!coordinatesId) {
            throw new Error('Координаты обязательны для обновления');
        }
        
        const updatePayload = {
            name: updatedData.name !== undefined ? updatedData.name : fullMarineData.name,
            health: updatedData.health !== undefined ? updatedData.health : fullMarineData.health,
            heartCount: updatedData.heartCount !== undefined ? updatedData.heartCount : fullMarineData.heartCount,
            category: updatedData.category !== undefined ? updatedData.category : fullMarineData.category,
            weaponType: updatedData.weaponType !== undefined ? updatedData.weaponType : fullMarineData.weaponType,
            coordinatesId: coordinatesId,
            chapterId: chapterId
        };
        
        console.log('Updating marine with payload:', updatePayload);
        
        const res = await fetch(`${API_BASE}/${marineId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatePayload)
        });
        
        if (res.ok) {
            cell.innerHTML = displayValue;
            // Обновляем data-original атрибут с правильным значением (ID для связанных объектов)
            const field = cell.dataset.field;
            if (field === 'coordinatesId' || field === 'chapterId') {
                cell.dataset.original = actualValue || '';
            } else {
                cell.dataset.original = actualValue;
            }
            // Обновляем данные в строке
            const row = cell.closest('tr');
            if (row.marineData) {
                Object.assign(row.marineData, updatedData);
            }
            // Обновляем selectedMarine если это тот же объект
            if (selectedMarine && selectedMarine.id === marineId) {
                Object.assign(selectedMarine, updatedData);
                // Гарантируем, что ID не потерян
                selectedMarine.id = marineId;
                console.log("selectedMarine обновлен после inline редактирования:", selectedMarine);
            }
        } else {
            const errorData = await res.json().catch(() => ({ error: res.statusText }));
            const errorMsg = errorData.error || errorData.message || `HTTP ${res.status}`;
            console.error('Update error:', errorData);
            throw new Error(`Ошибка обновления: ${errorMsg}`);
        }
    } catch (e) {
        await showAlert('Ошибка обновления: ' + e.message, 'Ошибка');
        cancelInlineEdit(cell, cell.dataset.original);
    }
}

function goToPage(page) {
    if (page >= 0 && page < totalPages) {
        currentPage = page;
        loadMarines();
    }
}

function applyFilter() {
    const nameFilter = document.getElementById("nameFilter").value.trim();
    currentFilter = nameFilter;
    currentPage = 0; // Сбрасываем на первую страницу при фильтрации
    loadMarines();
}

function clearFilter() {
    document.getElementById("nameFilter").value = "";
    currentFilter = "";
    currentPage = 0;
    loadMarines();
}


let isSaving = false; // Флаг для предотвращения двойной отправки

async function saveMarine(e) {
    e.preventDefault();
    
    // Защита от двойной отправки
    if (isSaving) {
        console.log("Форма уже отправляется, игнорируем повторный запрос");
        return;
    }
    
    const form = e.target;
    
    // Валидация обязательных полей
    if (!form.name.value.trim()) {
        await showAlert("Имя обязательно для заполнения", "Ошибка валидации");
        return;
    }
    if (!form.health.value || parseInt(form.health.value) < 1) {
        await showAlert("Health должно быть больше 0", "Ошибка валидации");
        return;
    }
    if (!form.heartCount.value || parseInt(form.heartCount.value) < 1 || parseInt(form.heartCount.value) > 3) {
        await showAlert("Heart Count должно быть от 1 до 3", "Ошибка валидации");
        return;
    }
    if (!form.category.value) {
        await showAlert("Категория обязательна для заполнения", "Ошибка валидации");
        return;
    }
    if (!form.weaponType.value) {
        await showAlert("Тип оружия обязателен для заполнения", "Ошибка валидации");
        return;
    }
    if (!form.coordinatesId.value) {
        await showAlert("Координаты обязательны для заполнения", "Ошибка валидации");
        return;
    }
    
    const coordinatesIdValue = form.coordinatesId.value;
    if (!coordinatesIdValue || isNaN(parseInt(coordinatesIdValue))) {
        await showAlert("Неверный ID координат", "Ошибка валидации");
        return;
    }

    const data = {
        name: form.name.value.trim(),
        health: parseInt(form.health.value),
        heartCount: parseInt(form.heartCount.value),
        category: form.category.value,
        weaponType: form.weaponType.value,
        coordinatesId: parseInt(coordinatesIdValue),
        chapterId: form.chapterId.value ? parseInt(form.chapterId.value) : null
    };

    // Отладка
    console.log("Отправляемые данные:", data);
    console.log("selectedMarine:", selectedMarine);
    console.log("Режим обновления:", selectedMarine && selectedMarine.id);

    try {
        // Проверяем, есть ли выделенный марин и его ID
        // Если selectedMarine установлен и имеет ID, это режим обновления
        const isUpdate = selectedMarine && selectedMarine.id;
        const selectedId = isUpdate ? selectedMarine.id : null; // Сохраняем ID для восстановления выделения
        
        console.log("=== saveMarine ===");
        console.log("selectedMarine:", selectedMarine);
        console.log("selectedMarine.id:", selectedMarine?.id);
        console.log("isUpdate:", isUpdate);
        console.log("selectedId:", selectedId);
        
        const url = isUpdate ? `${API_BASE}/${selectedMarine.id}` : API_BASE;
        const method = isUpdate ? "PUT" : "POST";
        
        console.log("URL:", url);
        console.log("Method:", method);
        console.log("==================");
        
        isSaving = true; // Устанавливаем флаг
        
        const res = await fetch(url, {
            method: method,
            headers: {"Content-Type":"application/json"},
            body: JSON.stringify(data)
        });
        if(!res.ok) {
            const errorText = await res.text();
            throw new Error("Ошибка сохранения: " + errorText);
        }
        await showAlert(isUpdate ? "✅ Десантник обновлён" : "✅ Десантник создан", "Успех");
        form.closest(".modal").style.display = "none";
        form.reset(); // Очистить форму
        
        // Загружаем данные и восстанавливаем выделение
        await loadMarines();
        
        if (isUpdate && selectedId) {
            // Восстанавливаем выделение после обновления
            setTimeout(() => {
                const rows = document.querySelectorAll("#marineTable tbody tr");
                for (let row of rows) {
                    const idCell = row.cells[0];
                    if (idCell && idCell.textContent.trim() === selectedId.toString()) {
                        document.querySelectorAll("#marineTable tr").forEach(r => r.classList.remove("selected"));
                        row.classList.add("selected");
                        
                        // Находим обновленный объект в данных и сохраняем как selectedMarine
                        fetch(`${API_BASE}/${selectedId}`)
                            .then(res => res.json())
                            .then(updatedMarine => {
                                selectedMarine = updatedMarine;
                            })
                            .catch(e => console.log("Не удалось загрузить обновленные данные", e));
                        break;
                    }
                }
            }, 100);
        } else {
            selectedMarine = null; // Сбросить выбор только для создания
        }
    } catch(e) { 
        await showAlert("Ошибка: " + e.message, "Ошибка"); 
    } finally {
        isSaving = false; // Сбрасываем флаг в любом случае
    }
}

function fillFormForEdit(marine) {
    const form = document.getElementById("marineForm");
    form.name.value = marine.name || "";
    form.health.value = marine.health || "";
    form.heartCount.value = marine.heartCount || "";
    form.category.value = marine.category || "";
    form.weaponType.value = marine.weaponType || "";
    form.coordinatesId.value = marine.coordinates?.id || "";
    form.chapterId.value = marine.chapter?.id || "";
}

async function deleteMarine() {
    if (!selectedMarine || !selectedMarine.id) {
        await showAlert("Выберите десантника для удаления", "Предупреждение");
        return;
    }
    
    // Сохраняем данные до выполнения асинхронных операций
    const marineId = selectedMarine.id;
    const marineName = selectedMarine.name;
    
    try {
        // Получаем информацию о связанных объектах
        const relatedRes = await fetch(`${API_BASE}/${marineId}/related`);
        if (!relatedRes.ok) {
            throw new Error("Не удалось получить информацию о связанных объектах");
        }
        const relatedData = await relatedRes.json();
        
        let deleteCoordinates = false;
        let deleteChapter = false;
        
        // Если есть связанные объекты, показываем диалог с выбором
        if (relatedData.hasCoordinates || relatedData.hasChapter) {
            const deleteOptions = await showDeleteConfirmationDialog(marineName, relatedData);
            if (!deleteOptions) {
                return;
            }
            deleteCoordinates = deleteOptions.deleteCoordinates;
            deleteChapter = deleteOptions.deleteChapter;
        } else {
            const confirmed = await showConfirm(`Удалить десантника "${marineName}"?`, "Подтверждение удаления");
            if (!confirmed) {
                return;
            }
        }
        
        // Отправляем запрос на удаление с параметрами
        const params = new URLSearchParams();
        if (deleteCoordinates) params.append('deleteCoordinates', 'true');
        if (deleteChapter) params.append('deleteChapter', 'true');
        
        const res = await fetch(`${API_BASE}/${marineId}?${params}`, {
            method: "DELETE"
        });
        if (!res.ok) {
            const errorText = await res.text();
            throw new Error("Ошибка удаления: " + errorText);
        }
        
        // Получаем подробную информацию об удалении
        const deleteResult = await res.json();
        await showAlert("✅ " + deleteResult.message, "Успех");
        
        selectedMarine = null;
        loadMarines();
    } catch(e) {
        await showAlert("Ошибка: " + e.message, "Ошибка");
    }
}

function showDeleteConfirmationDialog(marineName, relatedData) {
    return new Promise((resolve) => {
        // Создаем модальное окно для подтверждения удаления
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.style.display = 'block';
        
        let html = `
            <div class="modal-content" style="max-width: 600px;">
                <h2>Подтверждение удаления</h2>
                <p>Вы собираетесь удалить десантника: <strong>${marineName}</strong></p>
                
                <div class="related-objects-section">
                    <h3>Связанные объекты:</h3>
        `;
        
        // Показываем связанные координаты
        if (relatedData.hasCoordinates && relatedData.coordinates) {
            html += `
                <div class="related-object">
                    <label>
                        <input type="checkbox" id="deleteCoordinates">
                        Удалить координаты (x: ${relatedData.coordinates.x}, y: ${relatedData.coordinates.y})
                    </label>
                </div>
            `;
        }
        
        // Показываем связанную главу
        if (relatedData.hasChapter && relatedData.chapter) {
            html += `
                <div class="related-object">
                    <label>
                        <input type="checkbox" id="deleteChapter">
                        Удалить главу "${relatedData.chapter.name}" (десантников: ${relatedData.chapter.marinesCount})
                    </label>
                </div>
            `;
        }
        
        html += `
                </div>
                
                <div class="modal-buttons">
                    <button type="button" id="confirmDeleteBtn" class="btn-primary">Удалить</button>
                    <button type="button" id="cancelDeleteBtn" class="btn-secondary">Отмена</button>
                </div>
            </div>
        `;
        
        modal.innerHTML = html;
        document.body.appendChild(modal);
        
        // Обработчики событий
        document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
            // Собираем информацию о выбранных чекбоксах
            const deleteCoordinates = document.getElementById('deleteCoordinates')?.checked || false;
            const deleteChapter = document.getElementById('deleteChapter')?.checked || false;
            
            document.body.removeChild(modal);
            resolve({
                confirmed: true,
                deleteCoordinates,
                deleteChapter
            });
        });
        
        document.getElementById('cancelDeleteBtn').addEventListener('click', () => {
            document.body.removeChild(modal);
            resolve(null);
        });
        
        // Закрытие по клику вне модального окна
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                document.body.removeChild(modal);
                resolve(null);
            }
        });
    });
}

// Функции для работы с тумблером логирования статистики кэша
async function loadCacheStatsStatus() {
    try {
        const response = await fetch('/api/cache-stats/status');
        if (response.ok) {
            const data = await response.json();
            const toggle = document.getElementById('cacheStatsToggle');
            if (toggle) {
                toggle.checked = data.enabled;
            }
        }
    } catch (error) {
        console.error('Ошибка при загрузке состояния логирования кэша:', error);
    }
}

async function toggleCacheStats() {
    const toggle = document.getElementById('cacheStatsToggle');
    if (!toggle) return;
    
    const previousState = toggle.checked;
    
    try {
        const response = await fetch('/api/cache-stats/toggle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            toggle.checked = data.enabled;
            console.log('Логирование статистики кэша:', data.enabled ? 'включено' : 'отключено');
        } else {
            // В случае ошибки возвращаем тумблер в предыдущее состояние
            toggle.checked = previousState;
            console.error('Ошибка при переключении логирования кэша');
        }
    } catch (error) {
        // В случае ошибки возвращаем тумблер в предыдущее состояние
        toggle.checked = previousState;
        console.error('Ошибка при переключении логирования кэша:', error);
    }
}

// Инициализация тумблера при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    const toggle = document.getElementById('cacheStatsToggle');
    if (toggle) {
        // Загружаем состояние с бэка
        loadCacheStatsStatus();
        
        // Обработчик переключения
        toggle.addEventListener('change', toggleCacheStats);
    }
});