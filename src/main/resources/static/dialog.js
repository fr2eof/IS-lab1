// Утилита для создания диалоговых окон вместо alert/confirm/prompt

// Функция для создания модального диалога
function showDialog(options) {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'dialog-modal';
        modal.style.display = 'flex';
        
        const dialog = document.createElement('div');
        dialog.className = 'dialog-content';
        
        let html = '';
        if (options.title) {
            html += `<h2>${options.title}</h2>`;
        }
        if (options.message) {
            html += `<p>${options.message}</p>`;
        }
        
        // Если нужно поле ввода (prompt)
        if (options.input) {
            html += `<input type="${options.inputType || 'text'}" id="dialogInput" value="${options.defaultValue || ''}" style="width: 100%; padding: 8px; margin: 10px 0; box-sizing: border-box;">`;
        }
        
        // Кнопки
        html += '<div class="dialog-buttons">';
        if (options.type === 'confirm' || options.type === 'prompt') {
            html += '<button id="dialogOk" class="btn-primary">OK</button>';
            html += '<button id="dialogCancel" class="btn-secondary">Отмена</button>';
        } else {
            html += '<button id="dialogOk" class="btn-primary">OK</button>';
        }
        html += '</div>';
        
        dialog.innerHTML = html;
        modal.appendChild(dialog);
        document.body.appendChild(modal);
        
        // Фокус на поле ввода, если есть
        if (options.input) {
            setTimeout(() => {
                const input = dialog.querySelector('#dialogInput');
                if (input) {
                    input.focus();
                    input.select();
                }
            }, 100);
        }
        
        // Обработчики
        const okButton = dialog.querySelector('#dialogOk');
        const cancelButton = dialog.querySelector('#dialogCancel');
        
        const cleanup = () => {
            document.body.removeChild(modal);
        };
        
        okButton.addEventListener('click', () => {
            if (options.input) {
                const input = dialog.querySelector('#dialogInput');
                resolve(input.value);
            } else {
                resolve(true);
            }
            cleanup();
        });
        
        if (cancelButton) {
            cancelButton.addEventListener('click', () => {
                resolve(options.input ? null : false);
                cleanup();
            });
        }
        
        // Закрытие по клику вне диалога
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                resolve(options.input ? null : false);
                cleanup();
            }
        });
        
        // Закрытие по Escape
        const escapeHandler = (e) => {
            if (e.key === 'Escape') {
                resolve(options.input ? null : false);
                cleanup();
                document.removeEventListener('keydown', escapeHandler);
            }
        };
        document.addEventListener('keydown', escapeHandler);
        
        // Enter для подтверждения
        if (options.input) {
            const enterHandler = (e) => {
                if (e.key === 'Enter' && e.target.id === 'dialogInput') {
                    const input = dialog.querySelector('#dialogInput');
                    resolve(input.value);
                    cleanup();
                    document.removeEventListener('keydown', enterHandler);
                }
            };
            document.addEventListener('keydown', enterHandler);
        }
    });
}

// Удобные функции-обертки - делаем их доступными сразу
(function() {
    'use strict';
    
    // Определяем функции сразу, без async для немедленного присваивания
    window.showAlert = function(message, title) {
        title = title || 'Информация';
        return showDialog({ type: 'alert', message, title });
    };

    window.showConfirm = function(message, title) {
        title = title || 'Подтверждение';
        return showDialog({ type: 'confirm', message, title });
    };

    window.showPrompt = function(message, defaultValue, title, inputType) {
        title = title || 'Ввод';
        defaultValue = defaultValue || '';
        inputType = inputType || 'text';
        return showDialog({ type: 'prompt', message, title, input: true, defaultValue, inputType });
    };
})();

// Заменяем стандартные функции (опционально, для совместимости)
// Можно оставить их как есть и использовать новые функции напрямую

