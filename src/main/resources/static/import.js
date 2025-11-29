const API_IMPORT = "/api/import";

// Пример JSON для отображения
const exampleJson = {
    "spaceMarines": [
        {
            "name": "Marine 1",
            "coordinates": {
                "x": 10.5,
                "y": 20.3
            },
            "chapter": {
                "name": "Ultramarines",
                "marinesCount": 1
            },
            "health": 100,
            "heartCount": 2,
            "category": "ASSAULT",
            "weaponType": "BOLT_PISTOL"
        },
        {
            "name": "Marine 2",
            "coordinates": {
                "x": 15.0,
                "y": null
            },
            "health": 150,
            "heartCount": 3,
            "category": "TERMINATOR",
            "weaponType": "MULTI_MELTA"
        }
    ]
};

// Отображаем пример JSON
document.addEventListener('DOMContentLoaded', function() {
    const jsonExample = document.getElementById('jsonExample');
    if (jsonExample) {
        jsonExample.textContent = JSON.stringify(exampleJson, null, 2);
    }
});

function importFile() {
    const fileInput = document.getElementById('fileInput');
    const username = document.getElementById('username').value || 'user';
    const importBtn = document.getElementById('importBtn');

    if (!fileInput.files || fileInput.files.length === 0) {
        showStatus('Пожалуйста, выберите файл', 'error');
        return;
    }

    const file = fileInput.files[0];
    const reader = new FileReader();

    reader.onload = function(e) {
        // Отправляем содержимое файла на сервер
        importBtn.disabled = true;
        showStatus('Импорт в процессе...', 'info');

        fetch(`${API_IMPORT}/file?username=${encodeURIComponent(username)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: e.target.result
        })
        .then(async response => {
            // Проверяем Content-Type перед парсингом JSON
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                // Если ответ не JSON, читаем как текст
                const text = await response.text();
                let errorMessage = 'Ошибка импорта';
                if (text) {
                    if (text.includes('could not') || text.includes('Connection') || text.includes('database')) {
                        errorMessage = 'Ошибка подключения к базе данных. Проверьте, что база данных запущена.';
                    } else if (text.length < 200) {
                        errorMessage = 'Ошибка сервера: ' + text;
                    } else {
                        errorMessage = 'Ошибка сервера. Попробуйте позже.';
                    }
                }
                throw new Error(errorMessage);
            }
            
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.errorMessage || data.message || 'Ошибка импорта');
            }
            return data;
        })
        .then(data => {
            if (data.status === 'SUCCESS') {
                showStatus(`Успешно импортировано ${data.createdObjectsCount} объектов. ID операции: ${data.importHistoryId}`, 'success');
                fileInput.value = ''; // Очищаем выбор файла
                
                // Обновляем страницу Space Marines через WebSocket (если открыта)
                if (window.ws && window.ws.readyState === WebSocket.OPEN) {
                    window.ws.send('refresh');
                }
            } else {
                // Ошибка уже сохранена в историю на сервере
                showStatus(data.errorMessage || data.message || 'Ошибка импорта', 'error');
            }
        })
        .catch(error => {
            console.error('Import error:', error);
            const message = error.message || 'Ошибка при импорте файла';
            showStatus(message, 'error');
        })
        .finally(() => {
            importBtn.disabled = false;
        });
    };

    reader.onerror = function() {
        showStatus('Ошибка чтения файла', 'error');
        importBtn.disabled = false;
    };

    reader.readAsText(file);
}

function showStatus(message, type) {
    const statusDiv = document.getElementById('importStatus');
    statusDiv.textContent = message;
    statusDiv.className = 'status-message';
    
    switch(type) {
        case 'success':
            statusDiv.style.color = 'green';
            statusDiv.style.backgroundColor = '#d4edda';
            statusDiv.style.border = '1px solid #c3e6cb';
            break;
        case 'error':
            statusDiv.style.color = 'red';
            statusDiv.style.backgroundColor = '#f8d7da';
            statusDiv.style.border = '1px solid #f5c6cb';
            break;
        case 'info':
            statusDiv.style.color = 'blue';
            statusDiv.style.backgroundColor = '#d1ecf1';
            statusDiv.style.border = '1px solid #bee5eb';
            break;
    }
    
    statusDiv.style.padding = '10px';
    statusDiv.style.borderRadius = '5px';
    statusDiv.style.marginTop = '10px';
    statusDiv.style.display = 'block';
}

function downloadTemplate() {
    const template = {
        "spaceMarines": [
            {
                "name": "Пример десантника",
                "coordinates": {
                    "x": 10.5,
                    "y": 20.3
                },
                "chapter": {
                    "name": "Название ордена",
                    "marinesCount": 1
                },
                "health": 100,
                "heartCount": 2,
                "category": "ASSAULT",
                "weaponType": "BOLT_PISTOL"
            }
        ]
    };

    const blob = new Blob([JSON.stringify(template, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'import_template.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

