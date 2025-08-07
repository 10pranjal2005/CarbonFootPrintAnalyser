document.addEventListener('DOMContentLoaded', () => {
    const API_URL = 'http://localhost:8080';
    const loginForm = document.getElementById('loginForm');
    const quarterlyForm = document.getElementById('quarterlyForm');
    const getAnnualResultBtn = document.getElementById('getAnnualResult'); // Reference to the annual button
    
    // --- Authentication State Management ---
    const userEmail = localStorage.getItem('userEmail');
    const navCalculator = document.getElementById('nav-calculator');
    const navResults = document.getElementById('nav-results');
    const navLogin = document.getElementById('nav-login');
    const navLogout = document.getElementById('nav-logout');

    if (userEmail) {
        if(navCalculator) navCalculator.classList.remove('hidden');
        if(navResults) navResults.classList.remove('hidden');
        if(navLogin) navLogin.classList.add('hidden');
        if(navLogout) navLogout.classList.remove('hidden');
    } else {
        if(navCalculator) navCalculator.classList.add('hidden');
        if(navResults) navResults.classList.add('hidden');
        if(navLogin) navLogin.classList.remove('hidden');
        if(navLogout) navLogout.classList.add('hidden');
    }
    
    const protectedPages = ['calculator.html', 'results.html'];
    if (protectedPages.some(page => window.location.pathname.includes(page)) && !userEmail) {
        window.location.href = 'login.html';
    }

    // --- Event Listeners ---
    if (navLogout) {
        navLogout.addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.removeItem('userEmail');
            window.location.href = 'index.html';
        });
    }

    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            if (email) {
                localStorage.setItem('userEmail', email);
                document.getElementById('auth-message').textContent = 'Success! Redirecting...';
                setTimeout(() => { window.location.href = 'calculator.html'; }, 1000);
            }
        });
    }

    // --- Calculator Page Logic ---
    if (quarterlyForm) {
        const quarterlyResultDiv = document.getElementById('quarterlyResult');
        
        quarterlyForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const loggedInUser = localStorage.getItem('userEmail');
            if (!loggedInUser) {
                alert("Please log in first!");
                window.location.href = 'login.html';
                return;
            }

            const formData = new FormData(quarterlyForm);
            // THIS IS THE CRITICAL FIX: All form fields are now correctly read
            const data = {
                userEmail: loggedInUser,
                quarter: parseInt(formData.get('quarter')),
                electricity: parseFloat(formData.get('electricity')),
                gasoline: parseFloat(formData.get('gasoline')),
                naturalGas: parseFloat(formData.get('naturalGas')),
                waste: parseFloat(formData.get('waste')),
                meat: parseFloat(formData.get('meat')),
                lpgCylinders: parseFloat(formData.get('lpgCylinders')),
                waterHeating: parseFloat(formData.get('waterHeating')),
                shortFlights: parseFloat(formData.get('shortFlights')),
                longFlights: parseFloat(formData.get('longFlights')),
                publicTransportKm: parseFloat(formData.get('publicTransportKm')),
                taxiKm: parseFloat(formData.get('taxiKm')),
                dairy: parseFloat(formData.get('dairy')),
                vegDiet: parseFloat(formData.get('vegDiet')),
                veganDiet: parseFloat(formData.get('veganDiet')),
                eWaste: parseFloat(formData.get('eWaste')),
                streamingHours: parseFloat(formData.get('streamingHours')),
                cloudStorageGB: parseFloat(formData.get('cloudStorageGB')),
                newClothes: parseFloat(formData.get('newClothes')),
                bottledWater: parseFloat(formData.get('bottledWater'))
            };

            try {
                quarterlyResultDiv.textContent = 'Calculating...';
                const response = await fetch(`${API_URL}/calculate-quarter`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data),
                });

                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                
                const result = await response.json();
                quarterlyResultDiv.textContent = `Quarter ${result.quarter} Footprint: ${result.footprint.toFixed(2)} kg CO2e. Data saved!`;
            } catch (error) {
                quarterlyResultDiv.textContent = `Error: ${error.message}`;
            }
        });
    }
    
    // Logic for the Annual Footprint button RE-ADDED
    if(getAnnualResultBtn) {
        const annualResultDiv = document.getElementById('annualResult');
        getAnnualResultBtn.addEventListener('click', async () => {
             const loggedInUser = localStorage.getItem('userEmail');
             if (!loggedInUser) {
                 alert("Please log in first!");
                 window.location.href = 'login.html';
                 return;
             }

             try {
                annualResultDiv.textContent = 'Calculating...';
                const response = await fetch(`${API_URL}/get-annual-result`, {
                    headers: { 'X-User-Email': loggedInUser }
                });

                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

                const result = await response.json();
                annualResultDiv.textContent = `Total Annual Footprint: ${result.annualFootprint.toFixed(2)} kg CO2e. Stored in Sheet.`;
             } catch (error) {
                annualResultDiv.textContent = `Error: ${error.message}`;
             }
        });
    }
});