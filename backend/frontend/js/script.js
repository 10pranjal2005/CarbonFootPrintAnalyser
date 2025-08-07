document.addEventListener('DOMContentLoaded', () => {
    // --- All animation and calculator page logic is unchanged ---
    if (document.getElementById('particles-js')) { tsParticles.load("particles-js", { fpsLimit: 120, particles: { number: { value: 60, density: { enable: true, area: 800 } }, color: { value: ["#AFEEEE", "#ADD8E6", "#98FF98"], animation: { enable: true, speed: 50, sync: true } }, shape: { type: "circle" }, opacity: { value: 0.3 }, size: { value: { min: 1, max: 2.5 } }, links: { enable: true, distance: 180, color: "random", opacity: 0.2, width: 1, triangles: { enable: true, opacity: 0.05 } }, move: { enable: true, speed: 2, direction: "none", outModes: { default: "out" } } }, interactivity: { events: { onHover: { enable: true, mode: "grab" }, resize: true }, modes: { grab: { distance: 200, links: { opacity: 0.5 } } } }, detectRetina: true, background: { color: "#000000" } }); }
    const API_URL = 'http://localhost:8080';
    if (document.getElementById('footprintForm')) {
        const footprintForm = document.getElementById('footprintForm'); const quarterlyResultDiv = document.getElementById('quarterlyResult'); const sectionCards = document.querySelectorAll('.section-card'); const nextButtons = document.querySelectorAll('.next-btn');
        sectionCards.forEach(card => { card.addEventListener('click', () => { const targetId = card.getAttribute('data-target'); const targetSection = document.getElementById(targetId); const isVisible = targetSection.classList.contains('visible'); document.querySelectorAll('.form-section.visible').forEach(s => s.classList.remove('visible')); if (!isVisible) { targetSection.classList.add('visible'); } }); });
        nextButtons.forEach(button => { button.addEventListener('click', () => { const currentSection = button.closest('.form-section'); const nextCardId = button.getAttribute('data-next'); if (currentSection) currentSection.classList.remove('visible'); if (nextCardId) { const nextCard = document.getElementById(nextCardId); if (nextCard) { const nextSectionId = nextCard.getAttribute('data-target'); document.getElementById(nextSectionId)?.classList.add('visible'); } } }); });
        footprintForm.addEventListener('submit', async (e) => { e.preventDefault(); const data = { quarter: parseInt(document.getElementById('quarter').value), electricity: parseFloat(document.getElementById('electricity').value), naturalGas: parseFloat(document.getElementById('naturalGas').value), lpgCylinders: parseFloat(document.getElementById('lpgCylinders').value), waterHeating: parseFloat(document.getElementById('waterHeating').value), gasoline: parseFloat(document.getElementById('gasoline').value), flightsShort: parseFloat(document.getElementById('flightsShort').value), flightsLong: parseFloat(document.getElementById('flightsLong').value), publicTransport: parseFloat(document.getElementById('publicTransport').value), taxi: parseFloat(document.getElementById('taxi').value), twoWheeler: document.getElementById('twoWheeler').value, fourWheeler: document.getElementById('fourWheeler').value, meat: parseFloat(document.getElementById('meat').value), dairy: parseFloat(document.getElementById('dairy').value), vegetarianMonths: parseFloat(document.getElementById('vegetarianMonths').value), veganMonths: parseFloat(document.getElementById('veganMonths').value), waste: parseFloat(document.getElementById('waste').value), eWaste: parseFloat(document.getElementById('eWaste').value), clothes: parseFloat(document.getElementById('clothes').value), bottledWater: parseFloat(document.getElementById('bottledWater').value), streaming: parseFloat(document.getElementById('streaming').value), cloudStorage: parseFloat(document.getElementById('cloudStorage').value), mobile: document.getElementById('mobile').value, laptop: document.getElementById('laptop').value, region: document.getElementById('region').value, homeSize: parseFloat(document.getElementById('homeSize').value) }; try { const response = await fetch(`${API_URL}/calculate-quarter`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) }); if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`); const result = await response.json(); quarterlyResultDiv.textContent = `Quarter ${result.quarter} Footprint: ${result.footprint.toFixed(2)} kg CO2e`; } catch (error) { quarterlyResultDiv.textContent = `Error: ${error.message}`; } });
    }
    if (document.getElementById('getAnnualResult')) { const getAnnualResultBtn = document.getElementById('getAnnualResult'); const annualResultDiv = document.getElementById('annualResult'); getAnnualResultBtn.addEventListener('click', async () => { try { const response = await fetch(`${API_URL}/get-annual-result`); if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`); const result = await response.json(); annualResultDiv.textContent = `Total Annual Footprint: ${result.annualFootprint.toFixed(2)} kg CO2e`; } catch (error) { annualResultDiv.textContent = `Error: ${error.message}`; } }); }
    if (document.getElementById('footprintChart')) { const ctx = document.getElementById('footprintChart').getContext('2d'); const downloadBtn = document.getElementById('downloadCsv'); let allData = []; let footprintChart; async function fetchAndDisplayReport() { try { const response = await fetch(`${API_URL}/get-all-data`); if (!response.ok) throw new Error('Failed to fetch data from the server.'); allData = await response.json(); if (allData.length === 0) { ctx.font = "20px Montserrat"; ctx.fillStyle = "#888"; ctx.textAlign = "center"; ctx.fillText("No data submitted yet. Go to the calculator to add some!", ctx.canvas.width / 2, ctx.canvas.height / 2); return; } const quarterlyTotals = { 1: 0, 2: 0, 3: 0, 4: 0 }; allData.forEach(row => { const quarter = parseInt(row.Quarter); const footprint = parseFloat(row.Quarterly_Footprint); if (quarterlyTotals[quarter] !== undefined) { quarterlyTotals[quarter] += footprint; } }); const chartData = { labels: ['Quarter 1', 'Quarter 2', 'Quarter 3', 'Quarter 4'], datasets: [{ label: 'Quarterly Carbon Footprint (kg CO2e)', data: Object.values(quarterlyTotals), backgroundColor: (context) => { const chart = context.chart; const {ctx, chartArea} = chart; if (!chartArea) return null; const gradient = ctx.createLinearGradient(0, chartArea.bottom, 0, chartArea.top); gradient.addColorStop(0, '#0d3b66'); gradient.addColorStop(1, '#00c9b7'); return gradient; }, borderColor: '#00c9b7', borderWidth: 2, borderRadius: 5 }] }; if (footprintChart) footprintChart.destroy(); footprintChart = new Chart(ctx, { type: 'bar', data: chartData, options: { responsive: true, plugins: { legend: { display: false }, title: { display: true, text: 'Your Footprint by Quarter', color: '#fff', font: { size: 18 } } }, scales: { y: { beginAtZero: true, title: { display: true, text: 'kg CO2e', color: '#ccc' }, ticks: { color: '#ccc' }, grid: { color: '#333' } }, x: { ticks: { color: '#ccc' }, grid: { color: '#333' } } } } }); } catch (error) { console.error("Error fetching report data:", error); ctx.font = "20px Montserrat"; ctx.fillStyle = "#ff8080"; ctx.textAlign = "center"; ctx.fillText("Could not load report data.", ctx.canvas.width / 2, ctx.canvas.height / 2); } } function downloadCSV() { if (allData.length === 0) { alert("No data available to download."); return; } const headers = Object.keys(allData[0]); const csvContent = [ headers.join(','), ...allData.map(row => headers.map(header => JSON.stringify(row[header])).join(',')) ].join('\n'); const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' }); const link = document.createElement("a"); const url = URL.createObjectURL(blob); link.setAttribute("href", url); link.setAttribute("download", "carbon_footprint_report.csv"); link.style.visibility = 'hidden'; document.body.appendChild(link); link.click(); document.body.removeChild(link); } downloadBtn.addEventListener('click', downloadCSV); fetchAndDisplayReport(); }

    // --- NEW, RESILIENT LOGIC FOR THE GREEN SHIFT PAGE ---
    if (document.getElementById('recommendations-container')) {
        const container = document.getElementById('recommendations-container');
        
        async function fetchRecommendations() {
            try {
                // Create a timeout promise
                const timeoutPromise = new Promise((_, reject) =>
                    setTimeout(() => reject(new Error('The server is taking too long to respond.')), 10000) // 10 second timeout
                );

                // Race the fetch request against the timeout
                const response = await Promise.race([
                    fetch(`${API_URL}/get-green-shift`),
                    timeoutPromise
                ]);

                if (!response.ok) throw new Error('Could not fetch recommendations.');
                const recommendations = await response.json();

                if (recommendations.error) {
                     container.innerHTML = `<p class="loading-text">${recommendations.error}</p>`;
                     return;
                }
                
                if (recommendations.length === 0) {
                    container.innerHTML = `<p class="loading-text">Your footprint is looking good! Check back after submitting more data.</p>`;
                    return;
                }

                container.innerHTML = ''; 

                recommendations.forEach(rec => {
                    const card = document.createElement('div');
                    card.className = 'recommendation-card';
                    card.innerHTML = `
                        <h3>${rec.category}</h3>
                        <p>${rec.text}</p>
                        <div class="recommendation-meta">
                            <span class="savings-estimate">~${rec.savings} kg CO₂/year savings</span>
                            <span class="difficulty-tag ${rec.difficulty.toLowerCase()}">${rec.difficulty}</span>
                        </div>
                    `;
                    container.appendChild(card);
                });

            } catch (error) {
                console.error("Green Shift Error:", error); // Log the actual error for debugging
                container.innerHTML = `<p class="loading-text" style="color: #ff8080;">Could not load recommendations. The server may be busy or an error occurred. Please try again later.</p>`;
            }
        }
        
        fetchRecommendations();
    }
});