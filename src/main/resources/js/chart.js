google.charts.load('current', {'packages': ['corechart']});
google.setOnLoadCallback(fetchAndDraw);

function fetchAndDraw() {
    fetchLagsAndCallBack(fetchStatsAndDraw);//inside drawChart();
}

function drawChart(data1, data2, period) {
    //var data1 = new google.visualization.DataTable();
    //var data2 = new google.visualization.DataTable();
    //fillDataStats(data1);
    //fillDataLags(data2);

    var joinedData = google.visualization.data.join(data1, data2, 'full', [[0, 0]], [1, 2, 3, 4, 5, 6], [1]);

    var date_formatter = new google.visualization.DateFormat({
        pattern: "dd/MM/yyyy  HH:mm:ss"
    });
    date_formatter.format(joinedData, 0);  // Where 0 is the index of the column

    var stats_colors = ['#32CD32', '#A00000', '#00FFCC', '#87CEEB', '#F4A460', '#6A5ACD'];
    //var period = period;//'$-RDateTime';
    var maxOnline = 40;//20+(for tps)
    var wHeight = window.outerHeight;//inner
    var wWidth = window.outerWidth;

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
            title: period,
            format: 'HH:mm'
                    //,gridlines: {count: 30}
        },

        vAxes: {
            0: {logScale: false, maxValue: maxOnline, title: "Online & TPS", gridlines: {color: 'none'}},
            1: {logScale: false, minValue: 0, title: "Values & Lags"}
        },

        seriesType: 'lines',
        series: {
            0: {targetAxisIndex: 0},
            1: {targetAxisIndex: 1},
            2: {targetAxisIndex: 0},
            3: {targetAxisIndex: 1},
            4: {targetAxisIndex: 1},
            5: {targetAxisIndex: 1},
            6: {targetAxisIndex: 1, type: 'bars', color: '#ff0000'}
        },
        colors: stats_colors
    };

    //LineChart ComboChart
    var chart = new google.visualization.ComboChart(document.getElementById('chart_div'));
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
            title: period,
            format: 'HH:mm'
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
            title: period,
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

////#DATA_BEGIN
////Deprecated Static Specify firstSteps
//function fillDataStats(data) {
//
//    data.addColumn('datetime', 'DateTime');
//    data.addColumn('number', 'Tps');
//    data.addColumn('number', 'UsedMem(Mb)');
//    data.addColumn('number', 'Online');
//    data.addColumn('number', 'Chunks');
//    data.addColumn('number', 'Entities');
//    data.addColumn('number', 'Tiles');
//
//    //[new Date(1485000000000), 22.5, 33],
//    //loadStats(data);
//    data.addRows([// v1   v2  v3  v4  v5  v6
//        [new Date(2021, 10, 1), 23.8, 32, 44, 34, 12, 22],
//        [new Date(2021, 10, 3), 23.8, 33, 44, 35, 14, 24],
//        [new Date(2021, 10, 5), 24.2, 34, 34, 36, 16, 28],
//        [new Date(2021, 10, 7), 24.2, 35, 46, 37, 18, 30]
//    ]);
//}
//
////Deprecated Static Specify firstSteps
//function fillDataLags(data) {
//    data.addColumn('datetime', 'DateTime');
//    data.addColumn('number', 'lag');
//    data.addRows([//lag
//        [new Date(2021, 10, 1), 14.5],
//        [new Date(2021, 10, 2), 16.5],
//        [new Date(2021, 10, 4), 18.8]
//    ]);
//}
////#DATA_END

//datetime:DateTime number:Lags ...
function parseAndAnddColumns(lines, dataTable) {
    var columnPairs = lines[0].split(' ');
    for (let i = 0; i < columnPairs.length; i++) {
        var c = columnPairs[i].split(':');
        //ColumType:ColumName  dt.addColumn('datetime', 'DateTime'); .addColumn('number', 'Tps');
        dataTable.addColumn(c[0], c[1]);
    };    
}

function fetchLagsAndCallBack(callback) {    
    fetch('lags.bin')
            .then(response => response.text())
            .then(data => {
                //console.log(data);
                var lines = data.split('\n');
                var rows = [lines.length];
                if (lines < 2) {
                    console.log("Errornes data");
                    return;
                }
                
                var dataTableLags = new google.visualization.DataTable();
                parseAndAnddColumns(lines, dataTableLags);
                
                //Build Rows table for DataTable
                for (let k = 1; k < lines.length; k++) {
                    var line = lines[k].split(' ');
                    var chartRow = [line.length];
                    chartRow[0] = new Date(parseInt(line[0]));
                    chartRow[1] = parseInt(line[1]);
                    rows[k-1] = chartRow;
                }
                dataTableLags.addRows(rows);

                return callback(dataTableLags);//fetchStatsAndDraw
                
            }).catch(function(error) {
                console.log('Request failed', error);
            });;
}

//"callback"
function fetchStatsAndDraw(lagsDataTable) {
    fetch('stats.bin')
            .then(response => response.text())
            .then(data => {
                //console.log(data);
                var lines = data.split('\n');
                var rows = [lines.length];
                if (lines < 2) {
                    console.log("Errornes data");
                    return;
                }
                
                var statsDataTable = new google.visualization.DataTable();
                parseAndAnddColumns(lines, statsDataTable);
                
                //Build Rows table for DataTable
                var sDate, eDate;
                for (let k = 1; k < lines.length; k++) {
                    var line = lines[k].split(' ');
                    var chartRow = [line.length];
                    var time = parseInt(line[0]);
                    chartRow[0] = new Date(time);
                    chartRow[1] = parseInt(line[1]) / 10;//tps 200 -> 20.0
                    if (k===1) {
                        sDate = chartRow[0];
                    } else if (k===lines.length-1) {
                        eDate = chartRow[0];                        
                    }

                    for (let i = 2; i < line.length; i++) {
                         chartRow[i] = parseInt(line[i]);
                    }                    
                    //console.log(chartRow);
                    rows[k-1] = chartRow;
                }
                //[ [..],[..],[..]]
                statsDataTable.addRows(rows);
                
                //Now Load Lags Data and then Draw
                const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
                var period = sDate.toLocaleString(options) + "  -  " + eDate.toLocaleString(options);//'dd.MM.yy HH:mm:ss'; 
                
                return drawChart(statsDataTable, lagsDataTable, period);
                
            }).catch(function(error) {
                console.log('Request failed', error);
            });;
}
