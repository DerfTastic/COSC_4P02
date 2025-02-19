class TimeSortedMap {
    /**@type {ServerStatistics[]}*/
    data
    constructor() {
        this.data = []; 
    }

    /**
     * @param {ServerStatistics} stat 
     */
    insert(stat) {
        let index = this._findInsertionIndex(stat.curr_time_ms);
        this.data.splice(index, 0, stat);
    }

    /**
     * @param {number} start 
     * @param {number} stop 
     * @returns {ServerStatistics[]}
     */
    getRange(start, stop) {
        let startIndex = this._binarySearch(start);
        let stopIndex = this._binarySearch(stop + 1); // Exclusive upper bound
        return this.data.slice(startIndex, stopIndex);
    }

    /**
     * @param {number} time 
     * @returns {number}
     */
    _findInsertionIndex(time) {
        return this._binarySearch(time);
    }

    /**
     * @param {number} time 
     * @returns {number}
     */
    _binarySearch(time) {
        let left = 0, right = this.data.length;
        while (left < right) {
            let mid = Math.floor((left + right) / 2);
            if (this.data[mid].curr_time_ms < time) left = mid + 1;
            else right = mid;
        }
        return left;
    }
}

const stats = new TimeSortedMap();

async function collect_stats(){
    try{
        const result = await api.admin.get_server_statistics(cookies.getSession());
        stats.insert(result);
        update();
    }catch(e){
        console.log(e);
    }
}
setInterval(collect_stats, 250);

/**
 * 
 * @param {T} arr 
 * @param {*} func 
 */
function pairwise(arr, func){
    for(var i=0; i < arr.length - 1; i++){
        func(arr[i], arr[i + 1])
    }
}

function update(){
    const now = new Date();
    // gets all of the stats from 30 seconds ago to now
    const list = stats.getRange(now.getTime()-1000*30, now.getTime());

    if (list.length === 0) return;

    const latestStat = list[list.length - 1];
    const earliestStat = list[0];
    document.getElementById("totalRequests").innerText = latestStat.total_requests_handled;
    document.getElementById("requestsPerSecond").innerText = (latestStat.total_requests_handled-earliestStat.total_requests_handled)/(latestStat.curr_time_ms-earliestStat.curr_time_ms)*1000.0;

    const timestamps = list.map(stat => new Date(stat.curr_time_ms).toLocaleTimeString());
    const totalMemory = list.map(stat => stat.total_mem);
    const freeMemory = list.map(stat => stat.free_mem);
    const maxMemory = list.map(stat => stat.max_mem);
    

    {
        const datasets = [];
        Object.keys(latestStat.route_stats).forEach((route, index) => {
            const dataset = {
                idx: route,
                label: `Requests - ${route}/s`,
                borderColor: `hsl(${index * 50}, 70%, 50%)`,
                backgroundColor: `hsla(${index * 50}, 70%, 50%, 0.2)`,
                data: [],
                hidden: false
            };
            datasets.push(dataset);
        });   
        datasets.forEach(e => {
            e.data.push(NaN);
            pairwise(list, (earlier, later) => {
                let handled;
                if(Object.hasOwn(earlier.route_stats, e.idx)&&Object.hasOwn(later.route_stats, e.idx))
                    handled = earlier.route_stats[e.idx].requests_handled-later.route_stats[e.idx].requests_handled;
                else
                    handled = NaN;
                const time = earlier.curr_time_ms-later.curr_time_ms;
                e.data.push(handled/time*1000.0)
            });
        });
        updateChart(routeStatsChart, timestamps, datasets);
    }

    {
        const data = [];
        const datasets = [
            {
                label: `Requests/s`,
                backgroundColor: "red",
                data,
                hidden: false
            }
        ]; 
        data.push(NaN);
        pairwise(list, (earlier, later) => {
            let handled;
            handled = earlier.total_requests_handled-later.total_requests_handled;
            const time = earlier.curr_time_ms-later.curr_time_ms;
            data.push(handled/time*1000.0)
        });
        updateChart(requestsStatsChart, timestamps, datasets);
    }

    {
        const labels = [];
        const datasets = [
            {
                borderColor: [],
                backgroundColor: [],
                data: [],
                hidden: false
            }
        ];
        Object.keys(latestStat.route_stats).forEach((route, index) => {

            let early;
            try{
                early = earliestStat.route_stats[route];
            }catch(e){}
            if(early === undefined){
                early = {
                    total_response_time_ns: 0,
                    requests_handled: 0
                };
            }
            const total_time_ns = latestStat.route_stats[route].total_response_time_ns-early.total_response_time_ns;
            const total_handled = latestStat.route_stats[route].requests_handled-early.requests_handled;
            time = total_time_ns/total_handled/1e9*1000;
            
            
            labels.push(route);
            datasets[0].borderColor.push(`hsl(${index * 50}, 70%, 50%)`);
            datasets[0].backgroundColor.push(`hsl(${index * 50}, 70%, 50%, 0.2)`);
            datasets[0].data.push(time);
        });   
        updateChart(routeTimeChart, labels, datasets);
    }


    const rwStatements = list.map(stat => stat.db_stats.global.rw_prepared_statements_executed);
    const roStatements = list.map(stat => stat.db_stats.global.ro_prepared_statements_executed);
    
    {
        const datasets = [
            {
                label: `RW Prepared Statements/s`,
                borderColor: "red",
                data: [],
                hidden: false
            },
            {
                label: `RO Prepared Statements/s`,
                borderColor: "blue",
                data: [],
                hidden: false
            },
        ]; 
        datasets[0].data.push(NaN);
        datasets[1].data.push(NaN);
        pairwise(list, (earlier, later) => {
            const rw_handled = later.db_stats.global.rw_prepared_statements_executed-earlier.db_stats.global.rw_prepared_statements_executed;
            const ro_handled = later.db_stats.global.ro_prepared_statements_executed-earlier.db_stats.global.ro_prepared_statements_executed;
            const time = later.curr_time_ms-earlier.curr_time_ms;
            datasets[0].data.push(rw_handled/time*1000.0)
            datasets[1].data.push(ro_handled/time*1000.0)
        });
        updateChart(globalPreparedStatementsChart, timestamps, datasets);
    }
    
    updateChart(memoryUsageChart, timestamps, [
        { label: "Total Memory", data: totalMemory, borderColor: "green" },
        { label: "Max Memory", data: maxMemory, borderColor: "red" },
        { label: "Free Memory", data: freeMemory, borderColor: "orange" }
    ]);
}

function updateChart(chart, labels, datasets) {
    if (!chart) return;
    chart.data.labels = labels;
    datasets.forEach(e => {
        const found = chart.data.datasets.find(i => i.label === e.label);
        if(found){
            found.data = e.data;
        }else{
            chart.data.datasets.push(e);
        } 
    })
    // chart.data.datasets = datasets;
    chart.update();
}

function createChart(ctx, label, scales, type = 'line', ) {
    return new Chart(ctx, {
        type: type,
        data: {
            labels: [],
            datasets: []
        },
        options: {
            responsive: true,
            animation: false,
            plugins: {
                title: {
                    display: true,
                    text: label
                }
            },
            scales
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    Chart.defaults.backgroundColor = '#AAA';
    Chart.defaults.borderColor = '#AAA';
    Chart.defaults.color = '#AAA';
    Chart.defaults.elements.line.tension = 0.3;
    globalPreparedStatementsChart = createChart(document.getElementById("globalPreparedStatementsChart").getContext("2d"), "RW vs RO Statements Executed", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'Statements/s' },
            min: 0
        }
    });
    memoryUsageChart = createChart(document.getElementById("memoryUsageChart").getContext("2d"), "Memory Usage", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'bytes' },
            min: 0
        }
    });
    routeStatsChart = createChart(document.getElementById("routeStatsChart").getContext("2d"), "Route Statistics", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'Requests/s' },
            min: 0
        }
    });
    requestsStatsChart = createChart(document.getElementById("requestsStatsChart").getContext("2d"), "Requests", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'Requests/s' },
            min: 0
        }
    });
    routeTimeChart = new Chart(document.getElementById("routeTimeChart").getContext("2d"), {
        type: "bar",
        data: {
            labels: [],
            datasets: [
            ]
        },
        options: {
            indexAxis: 'y',
            elements: {
              bar: {
                borderWidth: 2,
              }
            },
        
            responsive: true,
            animation: false,
            plugins: {
                legend: {
                    display: false
                },
                title: {
                    display: true,
                    text: "Average Response Time(ms)"
                }
            }
        }
    });
});