const coordsPage = new CrudPage({
    apiBase: "/api/coordinates",
    tableId: "coordinatesTable",
    modalId: "coordModal",
    formId: "coordForm",
    fields: { x:{type:"number"}, y:{type:"number"} },
    wsEvents: ["coordinates_created", "coordinates_updated", "coordinates_deleted"] // WebSocket события для обновления
});

// Переопределяем метод save для обработки поля y
coordsPage.save = async function(e) {
    e.preventDefault();
    const form = e.target;
    const payload = {};
    Object.keys(this.fields).forEach(f => {
        let value = form[f].value;
        if(this.fields[f].type === "number") {
            if (f === 'y' && (value === '' || value === null || value === undefined)) {
                payload[f] = null; // Позволяем null для поля y
            } else {
                const numValue = parseFloat(value);
                payload[f] = isNaN(numValue) ? (f === 'y' ? null : 0) : numValue;
            }
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
};

// Делаем объект доступным глобально для пагинации
window.coordsPage = coordsPage;

// Переопределяем методы после создания объекта
document.addEventListener("DOMContentLoaded", () => {
    // Переопределяем setupWebSocketListener чтобы использовать наш load метод
    const originalSetupWS = coordsPage.setupWebSocketListener;
    coordsPage.setupWebSocketListener = function() {
        // Вызываем оригинальный setupWebSocketListener
        if (originalSetupWS) {
            originalSetupWS.call(this);
        }
        
        // Переопределяем wsHandler чтобы использовать наш load метод
        this.wsHandler = (message, action, id) => {
            if (this.wsEvents.includes(action)) {
                console.log(`WebSocket event ${action} received for ${this.apiBase}`);
                // Вызываем наш переопределенный load метод
                this.load();
                if (this.loadRelated) {
                    this.loadRelated();
                }
            }
        };
    };
    
    // Переподключаем WebSocket если нужно
    if (coordsPage.wsEvents.length > 0 && coordsPage.setupWebSocketListener) {
        coordsPage.setupWebSocketListener();
    }
    
    // Устанавливаем inline editing после загрузки данных с интервалом
    // чтобы убедиться, что таблица уже отрендерена
    const setupInlineEditing = () => {
        if (document.querySelector('#coordinatesTable tbody tr')) {
            setupCoordInlineEditing();
        } else {
            setTimeout(setupInlineEditing, 100);
        }
    };
    setTimeout(setupInlineEditing, 100);
});

coordsPage.renderRow = function(item) {
    return `
        <td>${item.id}</td>
        <td class="editable" data-field="x" data-original="${item.x}">${item.x}</td>
        <td class="editable" data-field="y" data-original="${item.y}">${item.y}</td>
    `;
};

// Переопределяем load для добавления inline editing
const originalLoadCoords = coordsPage.load.bind(coordsPage);
coordsPage.load = async function() {
    await originalLoadCoords();
    // Добавляем inline editing после загрузки данных
    setupCoordInlineEditing();
};

function setupCoordInlineEditing() {
    document.querySelectorAll('#coordinatesTable tbody tr').forEach(row => {
        const editableCells = row.querySelectorAll('.editable');
        editableCells.forEach(cell => {
            // Проверяем, что обработчик еще не установлен
            if (!cell.hasAttribute('data-inline-editing-setup')) {
                cell.addEventListener('dblclick', (e) => {
                    e.stopPropagation();
                    const coordId = row.cells[0].textContent;
                    const coordData = coordsPage.data.find(item => item.id.toString() === coordId);
                    coordsPage.startCoordEdit(cell, coordData);
                });
                cell.style.cursor = 'pointer';
                cell.title = 'Двойной клик для редактирования';
                cell.setAttribute('data-inline-editing-setup', 'true');
            }
        });
    });
}

coordsPage.startCoordEdit = function(cell, coordData) {
    const field = cell.dataset.field;
    const originalValue = cell.dataset.original;
    
    const input = document.createElement('input');
    input.type = 'number';
    input.value = originalValue;
    input.style.width = '100%';
    input.style.border = 'none';
    input.style.background = 'transparent';
    
    if (field === 'y') {
        input.step = 'any'; // Позволяем decimal для y
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
        if (newValue !== originalValue && (newValue !== '' || field === 'y')) {
            // Валидация
            if (field === 'x') {
                const numValue = parseFloat(newValue);
                if (isNaN(numValue)) {
                    isProcessing = false;
                    await showAlert(`${field.toUpperCase()} должно быть числом`, 'Ошибка валидации');
                    cell.innerHTML = originalValue;
                    return;
                }
            }
            // Для y разрешаем пустое значение (null)
            
            const confirmed = await showConfirm(`Изменить ${field} с "${originalValue}" на "${newValue || '(пусто)'}"?`, 'Подтверждение изменения');
            if (confirmed) {
                const updatedData = { ...coordData };
                if (field === 'x') {
                    updatedData.x = parseFloat(newValue);
                } else {
                    updatedData.y = newValue === '' ? null : parseFloat(newValue);
                }
                await this.updateCoordInline(coordData.id, updatedData, cell, newValue || 'null', originalValue);
            } else {
                cell.innerHTML = originalValue;
            }
        } else {
            cell.innerHTML = originalValue;
        }
        isProcessing = false;
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
            cell.innerHTML = originalValue;
        }
    });
};

coordsPage.updateCoordInline = async function(coordId, updatedData, cell, newValue, originalValue) {
    const savedOriginalValue = originalValue || cell.dataset.original;
    try {
        const res = await fetch(`${this.apiBase}/${coordId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedData)
        });
        
        if (res.ok) {
            cell.innerHTML = newValue;
            cell.dataset.original = newValue;
            // Обновляем данные в массиве
            const item = this.data.find(item => item.id === coordId);
            if (item) {
                Object.assign(item, updatedData);
            }
        } else {
            // Пытаемся получить сообщение об ошибке из ответа
            let errorMessage = 'Ошибка обновления';
            try {
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const errorData = await res.json();
                    errorMessage = errorData.error || errorData.message || errorMessage;
                } else {
                    const text = await res.text();
                    if (text) {
                        errorMessage = text;
                    }
                }
            } catch (parseError) {
                // Если не удалось распарсить ошибку, используем дефолтное сообщение
                errorMessage = `Ошибка обновления (${res.status})`;
            }
            throw new Error(errorMessage);
        }
    } catch (e) {
        await showAlert('Ошибка обновления: ' + e.message, 'Ошибка');
        cell.innerHTML = savedOriginalValue;
        cell.dataset.original = savedOriginalValue;
    }
};


document.getElementById("createCoordBtn").addEventListener("click", () => {
    coordsPage.selected = null; // Clear selection for create mode
    document.getElementById("coordForm").reset(); // Clear form
    coordsPage.modal.style.display="block";
});
document.getElementById("updateCoordBtn").addEventListener("click", async () => {
    if(!coordsPage.selected) { await showAlert("Выберите строку", "Предупреждение"); return; }
    const form = document.getElementById("coordForm");
    form.x.value = coordsPage.selected.x;
    form.y.value = coordsPage.selected.y;
    coordsPage.modal.style.display = "block";
});
document.getElementById("deleteCoordBtn").addEventListener("click", async () => {
    if(!coordsPage.selected || !coordsPage.selected.id) { await showAlert("Выберите строку", "Предупреждение"); return; }
    
    // Сохраняем данные до выполнения асинхронных операций
    const coordId = coordsPage.selected.id;
    const coordX = coordsPage.selected.x;
    const coordY = coordsPage.selected.y;
    
    try {
        // Получаем информацию о связанных объектах
        const relatedRes = await fetch(`${coordsPage.apiBase}/${coordId}/related`);
        if (!relatedRes.ok) {
            throw new Error("Не удалось получить информацию о связанных объектах");
        }
        const relatedData = await relatedRes.json();
        
        // Если есть связанные SpaceMarines, показываем диалог
        if (relatedData.relatedSpaceMarines && relatedData.relatedSpaceMarines.length > 0) {
            const shouldDelete = await showCoordinatesDeleteConfirmationDialog(`(${coordX}, ${coordY})`, relatedData.relatedSpaceMarines);
            if (!shouldDelete) {
                return;
            }
        } else {
            const confirmed = await showConfirm(`Удалить координаты (${coordX}, ${coordY})?`, "Подтверждение удаления");
            if (!confirmed) {
                return;
            }
        }
        
        const res = await fetch(`${coordsPage.apiBase}/${coordId}`, {method:"DELETE"});
        if(res.ok) { 
            await showAlert("✅ Координаты удалены", "Успех"); 
            coordsPage.load(); 
        } else {
            // Обрабатываем ошибку от сервера
            const errorData = await res.json();
            if (errorData.error) {
                await showAlert("❌ " + errorData.error, "Ошибка");
            } else {
                throw new Error("Ошибка удаления: " + res.status);
            }
        }
    } catch (e) {
        await showAlert("Ошибка: " + e.message, "Ошибка");
    }
});

// Обработчик изменения размера страницы
document.getElementById("pageSizeSelect").addEventListener("change", (e) => {
    coordsPage.pageSize = parseInt(e.target.value);
    coordsPage.currentPage = 0; // Сбрасываем на первую страницу
    coordsPage.load();
});

async function showCoordinatesDeleteConfirmationDialog(coordinatesStr, relatedMarines) {
    let message = `Вы собираетесь удалить координаты: ${coordinatesStr}`;
    if (relatedMarines.length > 0) {
        message += `\n\nСвязанные десантники будут автоматически удалены:\n`;
        relatedMarines.forEach(marine => {
            message += `• ${marine.name} (ID: ${marine.id})\n`;
        });
    }
    message += `\nПродолжить удаление?`;
    
    return await showConfirm(message, "Подтверждение удаления");
}
