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
 * @template T
 * @param {T[]} arr 
 * @param {function(T, T)} func 
 */
function pairwise(arr, func){
    for(var i=0; i < arr.length - 1; i++){
        func(arr[i], arr[i + 1])
    }
}

class Dataset{
    /** @type {string} */ label
    /** @type {string|string[]} */ borderColor
    /** @type {string|string[]} */ backgroundColor
    /** @type {number[]} */ data
    /** @type {boolean} */ hidden
}

function update(){
    const now = new Date();
    // gets all of the stats from 30 seconds ago to now
    const earlierTime = 0;//now.getTime()-1000*30;
    const list = stats.getRange(earlierTime, now.getTime());

    if (list.length < 2) return;

    const latestStat = list[list.length - 1];
    const earliestStat = list[0];
    document.getElementById("totalRequests").innerText = latestStat.total_requests_handled;
    document.getElementById("requestsPerSecond").innerText = (latestStat.total_requests_handled-earliestStat.total_requests_handled)/(latestStat.curr_time_ms-earliestStat.curr_time_ms)*1000.0;

    const timestamps = list.map(stat => new Date(stat.curr_time_ms).toLocaleTimeString());

    {
        /**
         * @type {Dataset[]}
         */
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

    {
        const datasets = [
            {
                label: `Statements/s`,
                borderColor: "green",
                data: [],
                hidden: false
            },
            {
                label: `RW Statements/s`,
                borderColor: "red",
                data: [],
                hidden: false
            },
            {
                label: `RO Statements/s`,
                borderColor: "blue",
                data: [],
                hidden: false
            },
        ]; 
        datasets[0].data.push(NaN);
        datasets[1].data.push(NaN);
        datasets[2].data.push(NaN);
        pairwise(list, (earlier, later) => {
            const rw_handled = later.db_stats.global.rw_prepared_statements_executed-earlier.db_stats.global.rw_prepared_statements_executed + later.db_stats.global.rw_statements_executed-earlier.db_stats.global.rw_statements_executed;
            const ro_handled = later.db_stats.global.ro_prepared_statements_executed-earlier.db_stats.global.ro_prepared_statements_executed + later.db_stats.global.ro_statements_executed-earlier.db_stats.global.ro_statements_executed;
            const time = later.curr_time_ms-earlier.curr_time_ms;
            datasets[0].data.push((rw_handled+ro_handled)/time*1000.0)
            datasets[1].data.push(rw_handled/time*1000.0)
            datasets[2].data.push(ro_handled/time*1000.0)
        });
        updateChart(globalStatementsChart, timestamps, datasets);
    }

    {
        const datasets = [
            {
                label: `Rw Held(ms)`,
                borderColor: "red",
                data: [],
                hidden: false
            },
            {
                label: `Ro Held(ms)`,
                borderColor: "green",
                data: [],
                hidden: false
            },
            {
                label: `Rw Acquire(ms)`,
                borderColor: "orange",
                data: [],
                hidden: false
            },
            {
                label: `Ro Acquire(ms)`,
                borderColor: "blue",
                data: [],
                hidden: false
            },
        ]; 
        datasets[0].data.push(NaN);
        datasets[1].data.push(NaN);
        datasets[2].data.push(NaN);
        datasets[3].data.push(NaN);
        pairwise(list, (earlier, later) => {
            const eg = earlier.db_stats.global;
            const lg = later.db_stats.global;
            
            const rw_held = (lg.rw_db_lock_held_ns-eg.rw_db_lock_held_ns)/(lg.rw_db_releases-eg.rw_db_releases);
            const ro_held = (lg.ro_db_lock_held_ns-eg.ro_db_lock_held_ns)/(lg.ro_db_releases-eg.ro_db_releases);
            const rw_acquire = (lg.rw_db_lock_waited_ns-eg.rw_db_lock_waited_ns)/(lg.rw_db_lock_waited-eg.rw_db_lock_waited);
            const ro_acquire = (lg.ro_db_lock_waited_ns-eg.ro_db_lock_waited_ns)/(lg.ro_db_lock_waited-eg.ro_db_lock_waited);
            
            datasets[0].data.push(rw_held/1e9*1000);
            datasets[1].data.push(ro_held/1e9*1000);
            datasets[2].data.push(rw_acquire/1e9*1000);
            datasets[3].data.push(ro_acquire/1e9*1000);
        });
        updateChart(globalTimingsChart, timestamps, datasets);
    }
    
    const totalMemory = list.map(stat => stat.total_mem);
    const freeMemory = list.map(stat => stat.free_mem);
    const maxMemory = list.map(stat => stat.max_mem);
    
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
            maintainAspectRatio: false,
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

document.addEventListener("resize", () => {
    Object.values(Chart.instances).forEach(e => e.resize());
})

document.addEventListener("DOMContentLoaded", () => {
    Chart.defaults.backgroundColor = '#AAA';
    Chart.defaults.borderColor = '#AAA';
    Chart.defaults.color = '#AAA';
    Chart.defaults.elements.line.tension = 0.3;
    globalStatementsChart = createChart(document.getElementById("globalStatementsChart").getContext("2d"), "RW vs RO Statements Executed", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'Statements/s' },
            min: 0
        }
    });
    globalTimingsChart = createChart(document.getElementById("globalTimingsChart").getContext("2d"), "Global Database Timing Stats", {
        x: {
            title: { display: true, text: 'Time' }
        },
        y: {
            title: { display: true, text: 'Average(ms)' },
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
            maintainAspectRatio: false,
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