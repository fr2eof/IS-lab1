const API_IMPORT = "/api/import";

let currentPage = 0;
let pageSize = 10;
let totalPages = 0;

document.addEventListener('DOMContentLoaded', function() {
    loadHistory();
});

function loadHistory() {
    const username = document.getElementById('usernameFilter').value || 'user';
    const isAdmin = document.getElementById('isAdminCheck').checked;
    pageSize = parseInt(document.getElementById('pageSizeSelect').value) || 10;
    
    // Убеждаемся, что currentPage - это число
    const page = typeof currentPage === 'number' ? currentPage : parseInt(currentPage) || 0;
    currentPage = page;

    const url = `${API_IMPORT}/history?username=${encodeURIComponent(username)}&isAdmin=${isAdmin}&page=${page}&size=${pageSize}`;

    fetch(url)
        .then(async response => {
            // Проверяем Content-Type
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                // Если ответ не JSON, читаем как текст
                const text = await response.text();
                let errorMessage = 'Ошибка загрузки истории';
                if (text) {
                    // Пытаемся извлечь понятное сообщение
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
            
            if (!response.ok) {
                // Пытаемся прочитать JSON с ошибкой
                try {
                    const errorData = await response.json();
                    throw new Error(errorData.errorMessage || errorData.error || errorData.message || 'Ошибка загрузки истории');
                } catch (e) {
                    if (e instanceof Error && e.message !== 'Unexpected token') {
                        throw e;
                    }
                    throw new Error('Ошибка загрузки истории. Код ответа: ' + response.status);
                }
            }
            return response.json();
        })
        .then(data => {
            // Spring Page object structure
            const content = data.content || data;
            displayHistory(content);
            const page = typeof data.number === 'number' ? data.number : currentPage;
            const totalElements = data.totalElements || 0;
            const size = data.size || pageSize;
            updatePagination(totalElements, page, size);
        })
        .catch(error => {
            console.error('Error loading history:', error);
            const message = error.message || 'Неизвестная ошибка при загрузке истории';
            showAlert(message, 'Ошибка');
        });
}

function displayHistory(history) {
    const tbody = document.querySelector('#historyTable tbody');
    tbody.innerHTML = '';

    if (history.length === 0) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 7;
        cell.textContent = 'История импорта пуста';
        cell.style.textAlign = 'center';
        return;
    }

    history.forEach(item => {
        const row = tbody.insertRow();
        
        const idCell = row.insertCell();
        idCell.textContent = item.id || '-';
        
        const userCell = row.insertCell();
        userCell.textContent = item.username || '-';
        
        const statusCell = row.insertCell();
        statusCell.textContent = item.status || '-';
        if (item.status === 'SUCCESS') {
            statusCell.style.color = 'green';
            statusCell.style.fontWeight = 'bold';
        } else if (item.status === 'FAILED') {
            statusCell.style.color = 'red';
            statusCell.style.fontWeight = 'bold';
        } else if (item.status === 'PENDING') {
            statusCell.style.color = 'orange';
            statusCell.style.fontWeight = 'bold';
        }
        
        const countCell = row.insertCell();
        if (item.status === 'SUCCESS' && item.createdObjectsCount !== null) {
            countCell.textContent = item.createdObjectsCount;
        } else {
            countCell.textContent = '-';
        }
        
        const dateCell = row.insertCell();
        if (item.createdAt) {
            const date = new Date(item.createdAt);
            dateCell.textContent = date.toLocaleString('ru-RU');
        } else {
            dateCell.textContent = '-';
        }
        
        const errorCell = row.insertCell();
        if (item.errorMessage) {
            errorCell.textContent = item.errorMessage;
            errorCell.style.color = 'red';
            errorCell.style.maxWidth = '300px';
            errorCell.style.overflow = 'hidden';
            errorCell.style.textOverflow = 'ellipsis';
            errorCell.title = item.errorMessage;
        } else {
            errorCell.textContent = '-';
        }
        
        const fileCell = row.insertCell();
        if (item.filePath) {
            const downloadLink = document.createElement('a');
            downloadLink.href = `/api/import/history/${item.id}/download?username=${encodeURIComponent(document.getElementById('usernameFilter').value || 'user')}&isAdmin=${document.getElementById('isAdminCheck').checked}`;
            downloadLink.textContent = 'Скачать';
            downloadLink.style.color = 'blue';
            downloadLink.style.textDecoration = 'underline';
            downloadLink.style.cursor = 'pointer';
            fileCell.appendChild(downloadLink);
        } else {
            fileCell.textContent = '-';
        }
    });
}

function updatePagination(total, page, size) {
    totalPages = Math.ceil(total / size);
    // Убеждаемся, что currentPage - это число
    currentPage = typeof page === 'number' ? page : parseInt(page) || 0;
    
    const paginationDiv = document.getElementById('pagination');
    paginationDiv.innerHTML = '';

    if (totalPages <= 1) {
        return;
    }

    // Кнопка "Предыдущая"
    const prevBtn = document.createElement('button');
    prevBtn.textContent = 'Предыдущая';
    prevBtn.disabled = currentPage === 0;
    prevBtn.onclick = () => {
        if (currentPage > 0) {
            currentPage--;
            loadHistory();
        }
    };
    paginationDiv.appendChild(prevBtn);

    // Номера страниц
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    if (startPage > 0) {
        const firstBtn = document.createElement('button');
        firstBtn.textContent = '1';
        firstBtn.onclick = () => {
            currentPage = 0;
            loadHistory();
        };
        paginationDiv.appendChild(firstBtn);

        if (startPage > 1) {
            const ellipsis = document.createElement('span');
            ellipsis.textContent = '...';
            ellipsis.style.padding = '0 5px';
            paginationDiv.appendChild(ellipsis);
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i + 1;
        pageBtn.disabled = i === currentPage;
        if (i !== currentPage) {
            pageBtn.onclick = () => {
                currentPage = i;
                loadHistory();
            };
        }
        paginationDiv.appendChild(pageBtn);
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const ellipsis = document.createElement('span');
            ellipsis.textContent = '...';
            ellipsis.style.padding = '0 5px';
            paginationDiv.appendChild(ellipsis);
        }

        const lastBtn = document.createElement('button');
        lastBtn.textContent = totalPages;
        lastBtn.onclick = () => {
            currentPage = totalPages - 1;
            loadHistory();
        };
        paginationDiv.appendChild(lastBtn);
    }

    // Кнопка "Следующая"
    const nextBtn = document.createElement('button');
    nextBtn.textContent = 'Следующая';
    nextBtn.disabled = currentPage >= totalPages - 1;
    nextBtn.onclick = () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadHistory();
        }
    };
    paginationDiv.appendChild(nextBtn);

    // Информация о странице
    const info = document.createElement('span');
    info.textContent = `Страница ${currentPage + 1} из ${totalPages} (всего: ${total})`;
    info.style.marginLeft = '20px';
    paginationDiv.appendChild(info);
}

