const chaptersPage = new CrudPage({
    apiBase: "/api/chapters",
    tableId: "chaptersTable",
    modalId: "chapterModal",
    formId: "chapterForm",
    fields: {
        name: {type:"text"},
        marinesCount: {type:"number"}
    },
    wsEvents: ["chapter_created", "chapter_updated", "chapter_deleted"]
});

window.chaptersPage = chaptersPage;

// Переопределяем методы после создания объекта
document.addEventListener("DOMContentLoaded", () => {
    const originalSetupWS = chaptersPage.setupWebSocketListener;
    chaptersPage.setupWebSocketListener = function() {
        if (originalSetupWS) {
            originalSetupWS.call(this);
        }
        
        // Переопределяем wsHandler чтобы использовать наш load метод
        this.wsHandler = (message, action, id) => {
            if (this.wsEvents.includes(action)) {
                console.log(`WebSocket event ${action} received for ${this.apiBase}`);
                this.load();
                if (this.loadRelated) {
                    this.loadRelated();
                }
            }
        };
    };
    
    if (chaptersPage.wsEvents.length > 0 && chaptersPage.setupWebSocketListener) {
        chaptersPage.setupWebSocketListener();
    }
    
    const setupInlineEditing = () => {
        if (document.querySelector('#chaptersTable tbody tr')) {
            setupChapterInlineEditing();
        } else {
            setTimeout(setupInlineEditing, 100);
        }
    };
    setTimeout(setupInlineEditing, 100);
});

chaptersPage.renderRow = function(item) {
    return `
        <td>${item.id}</td>
        <td class="editable" data-field="name" data-original="${item.name}">${item.name}</td>
        <td class="editable" data-field="marinesCount" data-original="${item.marinesCount}">${item.marinesCount}</td>
    `;
};

// Переопределяем load для добавления inline editing
const originalLoad = chaptersPage.load.bind(chaptersPage);
chaptersPage.load = async function() {
    await originalLoad();
    setupChapterInlineEditing();
};

function setupChapterInlineEditing() {
    document.querySelectorAll('#chaptersTable tbody tr').forEach(row => {
        const editableCells = row.querySelectorAll('.editable');
        editableCells.forEach(cell => {
            if (!cell.hasAttribute('data-inline-editing-setup')) {
                cell.addEventListener('dblclick', (e) => {
                    e.stopPropagation();
                    const chapterId = row.cells[0].textContent;
                    const chapterData = chaptersPage.data.find(item => item.id.toString() === chapterId);
                    chaptersPage.startChapterEdit(cell, chapterData);
                });
                cell.style.cursor = 'pointer';
                cell.title = 'Двойной клик для редактирования';
                cell.setAttribute('data-inline-editing-setup', 'true');
            }
        });
    });
}

chaptersPage.startChapterEdit = function(cell, chapterData) {
    const field = cell.dataset.field;
    const originalValue = cell.dataset.original;
    
    const input = document.createElement('input');
    input.type = field === 'marinesCount' ? 'number' : 'text';
    input.value = originalValue;
    if (field === 'marinesCount') {
        input.min = '1';
        input.max = '1000';
    }
    input.style.width = '100%';
    input.style.border = 'none';
    input.style.background = 'transparent';
    
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
            // Валидация для marinesCount
            if (field === 'marinesCount') {
                const numValue = parseInt(newValue);
                if (isNaN(numValue) || numValue < 1 || numValue > 1000) {
                    isProcessing = false;
                    await showAlert('Количество десантников должно быть от 1 до 1000', 'Ошибка валидации');
                    cell.innerHTML = originalValue;
                    return;
                }
            }
            
            const confirmed = await showConfirm(`Изменить ${field === 'marinesCount' ? 'количество десантников' : field} с "${originalValue}" на "${newValue}"?`, 'Подтверждение изменения');
            if (confirmed) {
                const updatedData = {};
                updatedData[field] = field === 'marinesCount' ? parseInt(newValue) : newValue;
                await this.updateChapterInline(chapterData.id, updatedData, cell, newValue);
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

chaptersPage.updateChapterInline = async function(chapterId, updatedData, cell, newValue) {
    try {
        // Сначала получаем полные данные главы
        const getRes = await fetch(`${this.apiBase}/${chapterId}`);
        const fullChapterData = await getRes.json();
        
        // Обновляем только нужные поля, используя новые значения из updatedData если они есть
        const updatePayload = {
            name: updatedData.name !== undefined ? updatedData.name : fullChapterData.name,
            marinesCount: updatedData.marinesCount !== undefined ? updatedData.marinesCount : fullChapterData.marinesCount
        };
        
        const res = await fetch(`${this.apiBase}/${chapterId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatePayload)
        });
        
        if (res.ok) {
            cell.innerHTML = newValue;
            cell.dataset.original = newValue;
            // Обновляем данные в массиве
            const item = this.data.find(item => item.id === chapterId);
            if (item) {
                Object.assign(item, updatedData);
            }
        } else {
            throw new Error('Ошибка обновления');
        }
    } catch (e) {
        await showAlert('Ошибка обновления: ' + e.message, 'Ошибка');
        cell.innerHTML = cell.dataset.original;
    }
};


document.getElementById("createChapterBtn").addEventListener("click", () => {
    chaptersPage.selected = null; // Clear selection for create mode
    document.getElementById("chapterForm").reset(); // Clear form
    chaptersPage.modal.style.display="block";
});
document.getElementById("updateChapterBtn").addEventListener("click", async () => {
    if(!chaptersPage.selected) { await showAlert("Выберите строку", "Предупреждение"); return; }
    const form = document.getElementById("chapterForm");
    form.name.value = chaptersPage.selected.name;
    form.marinesCount.value = chaptersPage.selected.marinesCount;
    chaptersPage.modal.style.display = "block";
});
document.getElementById("deleteChapterBtn").addEventListener("click", async () => {
    if(!chaptersPage.selected || !chaptersPage.selected.id) { await showAlert("Выберите строку", "Предупреждение"); return; }
    
    // Сохраняем данные до выполнения асинхронных операций
    const chapterId = chaptersPage.selected.id;
    const chapterName = chaptersPage.selected.name;
    
    try {
        // Получаем информацию о связанных объектах
        const relatedRes = await fetch(`${chaptersPage.apiBase}/${chapterId}/related`);
        if (!relatedRes.ok) {
            throw new Error("Не удалось получить информацию о связанных объектах");
        }
        const relatedData = await relatedRes.json();
        
        // Если есть связанные SpaceMarines, показываем диалог
        if (relatedData.relatedSpaceMarines && relatedData.relatedSpaceMarines.length > 0) {
            const shouldDelete = await showChapterDeleteConfirmationDialog(chapterName, relatedData.relatedSpaceMarines);
            if (!shouldDelete) {
                return;
            }
        } else {
            const confirmed = await showConfirm(`Удалить орден "${chapterName}"?`, "Подтверждение удаления");
            if (!confirmed) {
                return;
            }
        }
        
        const res = await fetch(`${chaptersPage.apiBase}/${chapterId}`, {method:"DELETE"});
        if(res.ok) { 
            await showAlert("✅ Орден удален", "Успех"); 
            chaptersPage.load(); 
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
    chaptersPage.pageSize = parseInt(e.target.value);
    chaptersPage.currentPage = 0; // Сбрасываем на первую страницу
    chaptersPage.load();
});

async function showChapterDeleteConfirmationDialog(chapterName, relatedMarines) {
    let message = `Вы собираетесь удалить орден: ${chapterName}`;
    if (relatedMarines.length > 0) {
        message += `\n\nСвязанные десантники будут автоматически удалены:\n`;
        relatedMarines.forEach(marine => {
            message += `• ${marine.name} (ID: ${marine.id})\n`;
        });
    }
    message += `\nПродолжить удаление?`;
    
    return await showConfirm(message, "Подтверждение удаления");
}
