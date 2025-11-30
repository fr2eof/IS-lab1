const API_IMPORT = "/api/import";

// Примеры JSON для отображения
const examples = {
    spacemarines: {
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
                "coordinatesId": 1,
                "chapterId": 2,
                "health": 150,
                "heartCount": 3,
                "category": "TERMINATOR",
                "weaponType": "MULTI_MELTA"
            },
            {
                "name": "Marine 3",
                "coordinates": {
                    "x": 15.0,
                    "y": null
                },
                "chapterId": 1,
                "health": 120,
                "heartCount": 2,
                "category": "SUPPRESSOR",
                "weaponType": "FLAMER"
            }
        ]
    },
    coordinates: {
        "coordinates": [
            {
                "x": 10.5,
                "y": 20.3
            },
            {
                "x": 15.0,
                "y": null
            },
            {
                "x": 20.7,
                "y": 30.1
            }
        ]
    },
    chapters: {
        "chapters": [
            {
                "name": "Ultramarines",
                "marinesCount": 1
            },
            {
                "name": "Blood Angels",
                "marinesCount": 5
            },
            {
                "name": "Dark Angels",
                "marinesCount": 10
            }
        ]
    }
};

// Отображаем примеры JSON при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Отображаем пример для Space Marines
    const spaceMarinesExample = document.getElementById('jsonExample');
    if (spaceMarinesExample && examples.spacemarines) {
        spaceMarinesExample.textContent = JSON.stringify(examples.spacemarines, null, 2);
    }
    
    // Отображаем пример для Coordinates
    const coordsExample = document.getElementById('jsonExampleCoords');
    if (coordsExample && examples.coordinates) {
        coordsExample.textContent = JSON.stringify(examples.coordinates, null, 2);
    }
    
    // Отображаем пример для Chapters
    const chaptersExample = document.getElementById('jsonExampleChapters');
    if (chaptersExample && examples.chapters) {
        chaptersExample.textContent = JSON.stringify(examples.chapters, null, 2);
    }
});

function importFile(type) {
    let fileInput, username, importBtn, statusId;
    
    if (type === 'spacemarines') {
        fileInput = document.getElementById('fileInput');
        username = document.getElementById('username').value || 'user';
        importBtn = document.getElementById('importBtn');
        statusId = 'importStatus';
    } else if (type === 'coordinates') {
        fileInput = document.getElementById('fileInputCoords');
        username = document.getElementById('usernameCoords').value || 'user';
        importBtn = document.getElementById('importBtnCoords');
        statusId = 'importStatusCoords';
    } else if (type === 'chapters') {
        fileInput = document.getElementById('fileInputChapters');
        username = document.getElementById('usernameChapters').value || 'user';
        importBtn = document.getElementById('importBtnChapters');
        statusId = 'importStatusChapters';
    } else {
        showStatus('Неизвестный тип импорта', 'error', statusId);
        return;
    }

    if (!fileInput.files || fileInput.files.length === 0) {
        showStatus('Пожалуйста, выберите файл', 'error', statusId);
        return;
    }

    const file = fileInput.files[0];
    const reader = new FileReader();

    reader.onload = function(e) {
        importBtn.disabled = true;
        showStatus('Импорт в процессе...', 'info', statusId);

        let endpoint = '';
        if (type === 'spacemarines') {
            endpoint = '/file';
        } else if (type === 'coordinates') {
            endpoint = '/coordinates';
        } else if (type === 'chapters') {
            endpoint = '/chapters';
        }

        // Для spacemarines используем старый метод /file, для остальных - JSON
        if (type === 'spacemarines') {
            fetch(`${API_IMPORT}${endpoint}?username=${encodeURIComponent(username)}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain'
                },
                body: e.target.result
            })
            .then(async response => {
                const contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('application/json')) {
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
                return response.json();
            })
            .then(data => {
                if (!data.status || data.status !== 'SUCCESS') {
                    throw new Error(data.errorMessage || data.message || 'Ошибка импорта');
                }
                const typeName = type === 'spacemarines' ? 'десантников' : 
                               type === 'coordinates' ? 'координат' : 'глав';
                showStatus(`Успешно импортировано ${data.createdObjectsCount} ${typeName}. ID операции: ${data.importHistoryId}`, 'success', statusId);
                fileInput.value = '';
                
                if (window.ws && window.ws.readyState === WebSocket.OPEN) {
                    window.ws.send('refresh');
                }
            })
            .catch(error => {
                console.error('Import error:', error);
                const message = error.message || 'Ошибка при импорте файла';
                showStatus(message, 'error', statusId);
            })
            .finally(() => {
                importBtn.disabled = false;
            });
        } else {
            // Для coordinates и chapters парсим JSON и отправляем
            try {
                const jsonContent = JSON.parse(e.target.result);
                fetch(`${API_IMPORT}${endpoint}?username=${encodeURIComponent(username)}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(jsonContent)
                })
                .then(async response => {
                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({ errorMessage: 'Ошибка импорта' }));
                        throw new Error(errorData.errorMessage || errorData.message || 'Ошибка импорта');
                    }
                    return response.json();
                })
                .then(data => {
                    const typeName = type === 'coordinates' ? 'координат' : 'глав';
                    showStatus(`Успешно импортировано ${data.createdObjectsCount} ${typeName}. ID операции: ${data.importHistoryId}`, 'success', statusId);
                    fileInput.value = '';
                    
                    if (window.ws && window.ws.readyState === WebSocket.OPEN) {
                        window.ws.send('refresh');
                    }
                })
                .catch(error => {
                    console.error('Import error:', error);
                    const message = error.message || 'Ошибка при импорте файла';
                    showStatus(message, 'error', statusId);
                })
                .finally(() => {
                    importBtn.disabled = false;
                });
            } catch (parseError) {
                showStatus('Ошибка парсинга JSON: ' + parseError.message, 'error', statusId);
                importBtn.disabled = false;
            }
        }
    };

    reader.onerror = function() {
        showStatus('Ошибка чтения файла', 'error', statusId);
        importBtn.disabled = false;
    };

    reader.readAsText(file);
}


function showStatus(message, type, statusId = 'importStatus') {
    const statusDiv = document.getElementById(statusId);
    if (!statusDiv) return;
    
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

function downloadTemplate(type) {
    let template, filename;
    
    if (type === 'spacemarines') {
        template = {
            "spaceMarines": [
                {
                    "name": "Пример десантника (создание новых координат и главы)",
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
                },
                {
                    "name": "Пример десантника (использование существующих по ID)",
                    "coordinatesId": 1,
                    "chapterId": 1,
                    "health": 150,
                    "heartCount": 3,
                    "category": "TERMINATOR",
                    "weaponType": "MULTI_MELTA"
                },
                {
                    "name": "Пример десантника (смешанный вариант)",
                    "coordinates": {
                        "x": 20.0,
                        "y": 30.5
                    },
                    "chapterId": 1,
                    "health": 120,
                    "heartCount": 2,
                    "category": "SUPPRESSOR",
                    "weaponType": "FLAMER"
                }
            ]
        };
        filename = 'import_spacemarines_template.json';
    } else if (type === 'coordinates') {
        template = {
            "coordinates": [
                {
                    "x": 10.5,
                    "y": 20.3
                },
                {
                    "x": 15.0,
                    "y": null
                }
            ]
        };
        filename = 'import_coordinates_template.json';
    } else if (type === 'chapters') {
        template = {
            "chapters": [
                {
                    "name": "Название ордена",
                    "marinesCount": 1
                },
                {
                    "name": "Другой орден",
                    "marinesCount": 5
                }
            ]
        };
        filename = 'import_chapters_template.json';
    } else {
        return;
    }

    const blob = new Blob([JSON.stringify(template, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
