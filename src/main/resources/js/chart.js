google.charts.load('current', {'packages': ['corechart']});
google.setOnLoadCallback(initFetchAndDrawOnReady);

function initFetchAndDrawOnReady() {
    fetchLagsAndCallBack();
    // \fetchCleanupsAndCallBack
    //   \fetchStatsAndDraw
    //     `->drawChart();
}

//datetime:DateTime number:Lags ...
function parseAndAndColumns(lines, dataTable) {
    var columnPairs = lines[0].split(' ');
    for (let i = 0; i < columnPairs.length; i++) {
        var c = columnPairs[i].split(':');
        //ColumType:ColumName  dt.addColumn('datetime', 'DateTime'); .addColumn('number', 'Tps');
        dataTable.addColumn(c[0], c[1]);
    };    
}

//Build Rows table for DataTable :: for lines millis value
function parseDateValueRows(lines) {
    var rows = [];    
    //0 - headers -> start from 1
    for (let k = 1; k < lines.length; k++) {
        var line = lines[k];
        if (line.length > 1 && line.indexOf(' ') !== -1) {
            var aline = lines[k].split(' ');
            var chartRow = [aline.length];
            chartRow[0] = new Date(parseInt(aline[0]));
            chartRow[1] = parseInt(aline[1]);
            rows.push(chartRow);
        }
    }
    return rows;   
}

//init fetch chain lags -> cleanups -> stats
//function fetchLagsAndCallBack(callback) {    
function fetchLagsAndCallBack() {    
    fetch('lags.txt', {cache: "no-cache"})
            .then(response => response.text())
            .then(data => {
                //console.log(data);
                var lines = data.split('\n');
                //var rows = [lines.length];
                if (lines < 2) {
                    console.log("Errornes data");
                    return;
                }
                
                var dataTableLags = new google.visualization.DataTable();
                parseAndAndColumns(lines, dataTableLags);
                var rows = parseDateValueRows(lines);
                
                //Build Rows table for DataTable
                dataTableLags.addRows(rows);

                //return callback(dataTableLags);//fetchStatsAndDraw
                fetchCleanupsAndCallBack(dataTableLags);
                
            }).catch(function(error) {
                console.log('Request failed', error);
            });;
}

function fetchCleanupsAndCallBack(dataTableLags) {    
    fetch('cleanups.txt', {cache: "no-cache"})
            .then(response => response.text())
            .then(data => {
                //console.log(data);
                var lines = data.split('\n');
                if (lines < 2) {
                    console.log("Errornes data");
                    return;
                }

                var dataTableCleanups = new google.visualization.DataTable();
                parseAndAndColumns(lines, dataTableCleanups);
                var rows = parseDateValueRows(lines);
                dataTableCleanups.addRows(rows);

                fetchStatsAndDraw(dataTableLags, dataTableCleanups);
                
            }).catch(function(error) {
                console.log('Request failed', error);
            });;
}

//"callback"
function fetchStatsAndDraw(lagsDataTable, cleanupsDataTable) {
    fetch('stats.txt', {cache: "no-cache"})
            .then(response => response.text())
            .then(data => {
                //console.log(data);
                var lines = data.split('\n');
                //var rows = [lines.length];
                if (lines < 2) {
                    console.log("Errornes data");
                    return;
                }
                
                var statsDataTable = new google.visualization.DataTable();
                parseAndAndColumns(lines, statsDataTable);
                
                //Build Rows table for DataTable
                var sDate, eDate;
                var rows = [];
                for (let k = 1; k < lines.length; k++) {
                    var line = lines[k].split(' ');
                    var chartRow = [line.length];
                    var time = parseInt(line[0]);
                    chartRow[0] = new Date(time);
                    chartRow[1] = parseInt(line[1]) / 10;//tps 200 -> 20.0
                    //DataTimeRegion for Chart 
                    if (k===1) {
                        sDate = chartRow[0];
                    } else if (k===lines.length-1) {
                        eDate = chartRow[0];                        
                    }

                    for (let i = 2; i < line.length; i++) {
                         chartRow[i] = parseInt(line[i]);
                    }                    
                    //console.log(chartRow);
                    //rows[k-1] = chartRow;
                    rows.push(chartRow);
                }
                //[ [..],[..],[..]]
                statsDataTable.addRows(rows);
                
                return drawChart(statsDataTable, lagsDataTable, cleanupsDataTable, sDate, eDate);
                
            }).catch(function(error) {
                console.log('Request failed', error);
            });;
}

/**
 * Draw three Charts
 * @param {google.visualization.DataTable()} data1 Stats
 * @param {google.visualization.DataTable()} data2 Lags
 * @param {google.visualization.DataTable()} data3 Cleanups
 * @param {Date} sDate 
 * @param {Date} eDate
 * @returns {undefined}
 */
function drawChart(data1, data2, data3, sDate, eDate) {
    
    const dateOptions = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
    var timeRangeHAxis = sDate.toLocaleString(dateOptions) + "  -  " + eDate.toLocaleString(dateOptions);//'dd.MM.yy HH:mm:ss'; 

    var statsAndLagsData = google.visualization.data.join(data1, data2, 'full', [[0, 0]], [1, 2, 3, 4, 5, 6], [1]);
    var joinedData = google.visualization.data.join(statsAndLagsData, data3, 'full', [[0, 0]], [1, 2, 3, 4, 5, 6, 7], [1]);

    var date_formatter = new google.visualization.DateFormat({
        pattern: "dd/MM/yyyy  HH:mm:ss"
    });
    date_formatter.format(joinedData, 0);  // Where 0 is the index of the column
    //                                                                           lags      cleanups
    var stats_colors = ['#32CD32', '#A00000', '#00FFCC', '#87CEEB', '#F4A460', '#6A5ACD', '#00FF00'];
    var maxOnline = 40;//20+(for tps)
    var wHeight = window.outerHeight;//inner
    var wWidth = window.outerWidth;

    //first Chart Stats&Lags
    var options = {
        title: 'Server Stats and Lags',
        //subtitle: 'Counters and Lags',
        legend: {position: 'top'},

        //override css
        width: wWidth,
        height: wHeight,
        interpolateNulls: true,
        explorer: {
            maxZoomOut: 1.5,
            maxZoomIn: .1
            //,keepInBounds: true
        },
        hAxis: {
            title: timeRangeHAxis,
            format: 'HH:mm'
            //,gridlines: {count: 30}
        },

        vAxes: {
            0: {logScale: false, maxValue: maxOnline, title: "Online & TPS", gridlines: {color: 'none'}},
            1: {logScale: false, minValue: 0, title: "Values & Lags & Cleanups"}
        },

        seriesType: 'lines',
        series: {
            0: {targetAxisIndex: 0},
            1: {targetAxisIndex: 1},
            2: {targetAxisIndex: 0},
            3: {targetAxisIndex: 1},
            4: {targetAxisIndex: 1},
            5: {targetAxisIndex: 1},
            6: {targetAxisIndex: 1, type: 'bars', color: '#ff0000'},//lags
            7: {targetAxisIndex: 1, type: 'bars', color: '#00ff00'}//cleanups
        },
        colors: stats_colors
    };

    //LineChart
    var chart = new google.visualization.ComboChart(document.getElementById('chart_stats_lags'));
    chart.draw(joinedData, options);

    //Lags Only
    var chartLags = new google.visualization.ComboChart(document.getElementById('chart_lags'));
    date_formatter.format(data2, 0);
    chartLags.draw(data2, {
        title: 'Server Tick Lags',
        legend: {position: 'top'},
        width: wWidth,
        height: wHeight,
        explorer: {
            maxZoomOut: 1.5,
            maxZoomIn: .1
        },
        hAxis: {
            title: timeRangeHAxis,
            format: 'HH:mm',
            //keep the time range corresponding with stats 
            viewWindow: { min: sDate, max: eDate }//new Date(2021,11,21)
        },
        vAxis: {
            title: 'Lag(ms) 1,000 = 1 sec',
            minValue: 0
        },
        seriesType: 'bars',
        series: {
            0: {color: '#ff0000'}
        }
    });
    
    //Cleanups
    var chartCleanups = new google.visualization.ComboChart(document.getElementById('chart_cleanups'));
    date_formatter.format(data3, 0);
    chartCleanups.draw(data3, {
        title: 'Optiserver Cleanups',
        legend: {position: 'top'},
        width: wWidth,
        height: wHeight,
        explorer: {
            maxZoomOut: 1.5,
            maxZoomIn: .1
        },
        hAxis: {
            title: timeRangeHAxis,
            format: 'HH:mm',
            viewWindow: { min: sDate, max: eDate }
        },
        vAxis: {
            title: 'Took(ms) 1,000 = 1 sec',
            minValue: 0
        },
        seriesType: 'bars',
        series: {
            0: {color: '#00ff00'}
        }
    });

    //StatsOnly
    var chartLags = new google.visualization.LineChart(document.getElementById('chart_stats'));
    date_formatter.format(data2, 0);
    chartLags.draw(data1, {
        title: 'Server Stats',
        legend: {position: 'top'},
        width: wWidth,
        height: wHeight,
        explorer: {
            maxZoomOut: 1.5,
            maxZoomIn: .1
        },
        hAxis: {
            title: timeRangeHAxis,
            format: 'HH:mm'
        },
        vAxes: {
            0: {logScale: false, maxValue: maxOnline, title: "Online & TPS", gridlines: {color: 'none'}},
            1: {logScale: false, minValue: 0, title: "Values & Lags"}
        },
        series: {
            0: {targetAxisIndex: 0}, //tps
            1: {targetAxisIndex: 1},
            2: {targetAxisIndex: 0}, //online
            3: {targetAxisIndex: 1},
            4: {targetAxisIndex: 1},
            5: {targetAxisIndex: 1}
        },
        colors: stats_colors
    });
}
