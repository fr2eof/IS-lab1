const API_BASE = "http://localhost:8080/lab1-1.0-SNAPSHOT/api";
const API_SPECIAL_OPS = `${API_BASE}/special-operations`;
const API_SPACE_MARINES = `${API_BASE}/spacemarines`;

document.addEventListener("DOMContentLoaded", () => {
    // Операция 1: Среднее значение heartCount
    document.getElementById("avgHeartCountBtn").addEventListener("click", async () => {
        try {
            const res = await fetch(`${API_SPECIAL_OPS}/average-heart-count`);
            if (!res.ok) {
                throw new Error("Ошибка выполнения запроса");
            }
            const data = await res.json();
            
            const resultDiv = document.getElementById("avgHeartCountResult");
            const valueDiv = document.getElementById("avgHeartCountValue");
            valueDiv.textContent = `Среднее значение heartCount: ${data.average.toFixed(2)}`;
            resultDiv.style.display = "block";
        } catch (e) {
            await showAlert("Ошибка: " + e.message, "Ошибка");
        }
    });

    // Операция 2: Количество десантников с health меньше заданного
    document.getElementById("healthLessBtn").addEventListener("click", async () => {
        const healthValue = document.getElementById("healthValue").value.trim();
        if (!healthValue || isNaN(parseInt(healthValue)) || parseInt(healthValue) < 1) {
            await showAlert("Введите корректное значение health (число больше 0)", "Ошибка ввода");
            return;
        }
        
        try {
            const res = await fetch(`${API_SPECIAL_OPS}/count-by-health?health=${healthValue}`);
            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(errorText || "Ошибка выполнения запроса");
            }
            const data = await res.json();
            
            const resultDiv = document.getElementById("healthLessResult");
            const valueDiv = document.getElementById("healthLessValue");
            valueDiv.textContent = `Количество десантников с health < ${healthValue}: ${data.count}`;
            resultDiv.style.display = "block";
        } catch (e) {
            await showAlert("Ошибка: " + e.message, "Ошибка");
        }
    });

    // Операция 3: Поиск по имени
    document.getElementById("findByNameBtn").addEventListener("click", async () => {
        const nameValue = document.getElementById("nameSearchValue").value.trim();
        if (!nameValue) {
            await showAlert("Введите подстроку для поиска", "Ошибка ввода");
            return;
        }
        
        try {
            const res = await fetch(`${API_SPECIAL_OPS}/search-by-name?name=${encodeURIComponent(nameValue)}`);
            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(errorText || "Ошибка выполнения запроса");
            }
            const data = await res.json();
            
            const resultDiv = document.getElementById("findByNameResult");
            const listDiv = document.getElementById("findByNameList");
            
            if (!data.marines || data.marines.length === 0) {
                listDiv.innerHTML = '<p style="color: #666;">Ничего не найдено</p>';
            } else {
                let html = `<p><strong>Найдено десантников: ${data.marines.length}</strong></p>`;
                html += '<table class="result-table"><thead><tr><th>Имя</th><th>ID</th></tr></thead><tbody>';
                data.marines.forEach(marine => {
                    html += `<tr><td>${marine[0]}</td><td>${marine[1]}</td></tr>`;
                });
                html += '</tbody></table>';
                listDiv.innerHTML = html;
            }
            resultDiv.style.display = "block";
        } catch (e) {
            await showAlert("Ошибка: " + e.message, "Ошибка");
        }
    });

    // Операция 4: Отчисление из ордена
    document.getElementById("removeFromChapterBtn").addEventListener("click", async () => {
        const marineId = document.getElementById("marineIdForRemoval").value.trim();
        if (!marineId || isNaN(parseInt(marineId)) || parseInt(marineId) < 1) {
            await showAlert("Введите корректный ID десантника", "Ошибка ввода");
            return;
        }
        
        // Проверяем, есть ли у десантника орден
        try {
            const marineRes = await fetch(`${API_SPACE_MARINES}/${marineId}`);
            if (!marineRes.ok) {
                throw new Error("Десантник не найден");
            }
            const marine = await marineRes.json();
            
            if (!marine.chapter || !marine.chapter.id) {
                await showAlert("У выбранного десантника нет ордена", "Ошибка");
                return;
            }
            
            const confirmed = await showConfirm(
                `Отчислить десантника "${marine.name}" из ордена "${marine.chapter.name}"?`,
                "Подтверждение отчисления"
            );
            
            if (!confirmed) {
                return;
            }
            
            const res = await fetch(`${API_SPACE_MARINES}/${marineId}/remove-from-chapter`, {
                method: "PUT"
            });
            
            if (!res.ok) {
                const errorText = await res.text();
                throw new Error("Ошибка отчисления: " + errorText);
            }
            
            const resultDiv = document.getElementById("removeFromChapterResult");
            const messageDiv = document.getElementById("removeFromChapterMessage");
            messageDiv.innerHTML = `<p style="color: #28a745; font-weight: bold;">✅ Десантник "${marine.name}" отчислен из ордена "${marine.chapter.name}"</p>`;
            resultDiv.style.display = "block";
            
            // Перезагружаем данные на главной странице (если она открыта)
            if (typeof loadMarines === 'function') {
                loadMarines();
            }
        } catch (e) {
            await showAlert("Ошибка: " + e.message, "Ошибка");
        }
    });
});

