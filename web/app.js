let chartInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    fetchState();

    // Auto-randomize ID logic to prevent accidental velocity blocks during demo
    document.getElementById('btn-randomize').addEventListener('click', randomizeId);
    
    document.getElementById('payment-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const payload = {
            customerId: document.getElementById('customerId').value,
            merchantName: document.getElementById('merchantName').value,
            amount: parseFloat(document.getElementById('amount').value),
            paymentMethod: document.getElementById('paymentMethod').value,
            location: document.getElementById('location').value,
            strategy: document.getElementById('strategy').value
        };

        const res = await fetch('/api/transactions/process', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        
        const data = await res.json();
        renderResult(data);
        fetchState();
        
        // Auto-change option execution after Process
        randomizeId(); 
    });
});

function randomizeId() {
    document.getElementById('customerId').value = 'CUST-' + Math.floor(Math.random() * 10000);
}

async function fetchState() {
    const res = await fetch('/api/state');
    const data = await res.json();
    renderGateways(data.gateways);
    renderAnalytics(data.analytics);
}

function renderGateways(gateways) {
    const container = document.getElementById('gateways-container');
    container.innerHTML = '';
    
    gateways.forEach(g => {
        const card = document.createElement('div');
        card.className = 'gateway-card';
        card.innerHTML = `
            <h3>${g.name}</h3>
            <label>Fee: <span id="fee-val-${g.name}">${g.fee}</span>%</label>
            <input type="range" min="0" max="5" step="0.1" value="${g.fee}" onchange="updateGateway('${g.name}', this.value, document.getElementById('sr-${g.name}').value, document.getElementById('en-${g.name}').checked)">
            
            <label>Success Rate: <span id="sr-val-${g.name}">${g.successRate}</span>%</label>
            <input type="range" id="sr-${g.name}" min="80" max="100" step="0.1" value="${g.successRate}" onchange="updateGateway('${g.name}', document.querySelector('input[value=\\'${g.fee}\\']').value, this.value, document.getElementById('en-${g.name}').checked)">
            
            <label style="margin-top:10px;">
                <input type="checkbox" id="en-${g.name}" ${g.enabled ? 'checked' : ''} onchange="updateGateway('${g.name}', document.querySelector('input[value=\\'${g.fee}\\']').value, document.getElementById('sr-${g.name}').value, this.checked)">
                Active / Enabled
            </label>
        `;
        container.appendChild(card);
    });
}

async function updateGateway(name, fee, successRate, enabled) {
    await fetch('/api/gateways/update', {
        method: 'POST',
        body: JSON.stringify({ name, fee: parseFloat(fee), successRate: parseFloat(successRate), enabled })
    });
    fetchState();
}

function renderResult(tx) {
    const badges = document.getElementById('result-badges');
    const statusClass = tx.status === 'SETTLED' ? 'badge-success' : 'badge-error';
    
    badges.innerHTML = `
        <span class="badge-info">ID: ${tx.txId}</span>
        <span class="${statusClass}">Status: ${tx.status}</span>
        <span class="badge-info">Gateway: ${tx.selectedGateway}</span>
        <span class="badge-info">Fee: ${tx.appliedFee}%</span>
        <span class="${tx.fraudResult === 'PASSED' ? 'badge-success' : 'badge-error'}">Fraud: ${tx.fraudResult}</span>
        <span class="badge-info">Time: ${tx.executionTimeMs}ms</span>
    `;

    const trail = document.getElementById('audit-trail');
    trail.innerHTML = '';
    tx.auditTrail.forEach(a => {
        const li = document.createElement('li');
        li.innerText = `[${a.state}] ${a.message}`;
        trail.appendChild(li);
    });
}

function renderAnalytics(analytics) {
    document.getElementById('stat-total').innerText = analytics.total;
    document.getElementById('stat-settled').innerText = analytics.settled;
    document.getElementById('stat-failed').innerText = analytics.failed;

    const ctx = document.getElementById('analyticsChart').getContext('2d');
    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Settled', 'Failed'],
            datasets: [{
                data: [analytics.settled, analytics.failed],
                backgroundColor: ['#03dac6', '#cf6679'],
                borderWidth: 0
            }]
        },
        options: {
            plugins: { legend: { labels: { color: '#fff' } } }
        }
    });
}