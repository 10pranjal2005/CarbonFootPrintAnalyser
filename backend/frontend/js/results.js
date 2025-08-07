document.addEventListener('DOMContentLoaded', () => {
    const API_URL = 'http://localhost:8080';
    const userEmail = localStorage.getItem('userEmail');

    if (!userEmail) {
        window.location.href = 'login.html';
        return;
    }

    const resultsSummaryDiv = document.getElementById('results-summary');
    const downloadCsvBtn = document.getElementById('downloadCsv');
    let allReportsData = [];

    async function fetchReports() {
        try {
            const response = await fetch(`${API_URL}/get-reports`, {
                headers: { 'X-User-Email': userEmail }
            });
            if (!response.ok) throw new Error('Failed to fetch reports.');
            
            const reports = await response.json();
            allReportsData = reports.data;
            
            if (!allReportsData || allReportsData.length === 0) {
                resultsSummaryDiv.innerHTML = '<p>No reports found. Go to the <a href="calculator.html">calculator</a> to submit your first one!</p>';
                return;
            }
            
            displaySummary(allReportsData);
            renderChart(allReportsData);

        } catch (error) {
            resultsSummaryDiv.innerHTML = `<p style="color: red;">Error loading data: ${error.message}</p>`;
        }
    }

    function displaySummary(reports) {
        // --- THIS IS THE CRITICAL FIX ---
        // The key now matches the sanitized key from the Java backend
        const totalFootprint = reports.reduce((sum, report) => sum + parseFloat(report.QuarterlyFootprintkgCO2e), 0);
        // --- END OF FIX ---
        const averageFootprint = totalFootprint / reports.length;
        
        resultsSummaryDiv.innerHTML = `
            <p><strong>Total Submissions:</strong> ${reports.length}</p>
            <p><strong>Average Quarterly Footprint:</strong> ${averageFootprint.toFixed(2)} kg CO₂e</p>
        `;
    }

    function renderChart(reports) {
        const ctx = document.getElementById('footprintChart').getContext('2d');
        
        const labels = reports.map(r => `Q${r.Quarter} (${r.Timestamp.split(' ')[0]})`);
        // --- THIS IS THE CRITICAL FIX ---
        const data = reports.map(r => r.QuarterlyFootprintkgCO2e);
        // --- END OF FIX ---

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Quarterly Footprint (kg CO₂e)',
                    data: data,
                    backgroundColor: 'rgba(42, 157, 143, 0.7)',
                    borderColor: 'rgba(38, 70, 83, 1)',
                    borderWidth: 1,
                    borderRadius: 5
                }]
            },
            options: {
                scales: { y: { beginAtZero: true, title: { display: true, text: 'kg CO₂e' } } },
                responsive: true,
                plugins: {
                    legend: { position: 'top' },
                    title: { display: true, text: 'Your Carbon Footprint History', font: { size: 18 } }
                }
            }
        });
    }
    
    downloadCsvBtn.addEventListener('click', () => {
        if (allReportsData.length === 0) {
            alert("No data available to download.");
            return;
        }

        const headers = Object.keys(allReportsData[0]);
        const csvContent = [
            headers.join(','),
            ...allReportsData.map(row => 
                headers.map(header => `"${row[header]}"`).join(',')
            )
        ].join('\n');
        
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', 'carbonwise_report.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    });

    fetchReports();
});