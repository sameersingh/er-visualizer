
var parseDate = d3.time.format('%x');
var entities = [];
//var scale = 604800; // per week
var scale = 86400; // day

function parseData(d) {
    var clusters = Math.max.apply(null, _.pluck(d, "ci"))
    var data = [];
    for (i=0; i < clusters; i++) {
      data[i] = [];
    }

    var maxTimestamp = 0;
    var minTimestamp = 999999999999;

    d.map(function(e,i) {
      if (e.timestamp > maxTimestamp) {
        maxTimestamp = e.timestamp;
      }
      if (e.timestamp < minTimestamp) {
        minTimestamp = e.timestamp;
      }
      e.lambdas.map(function (c, i) {
        data[c.cj-1].push({x: e.timestamp, y: c.dec});
        data[c.cj-1].push({x: e.timestamp, y: c.inc});
      });
    });

    var bins = Math.round((maxTimestamp - minTimestamp) / scale);
    console.log(bins);

    xs = []
    vitals = []
    non_vitals = []
    for (i=0; i < bins; i++) {
      xs.push(Math.round(minTimestamp + (scale * i)));
      vitals.push(0);
      non_vitals.push(0);
    }
    
    d.map(function(e,i) {
      var bin = Math.round((e.timestamp - minTimestamp) / scale);
      if (e.relevance == 2) {
          vitals[bin] += 1;
      } else {
          non_vitals[bin] += 1;
      }
    });

    var staleness = data.map(function(cluster, i) {
      return {
        key: "C" + (i+1),
        values: cluster
      };
    });

    vitals_values = [];
    non_vitals_values = [];
    for (i=0; i < bins; i++) {
      vitals_values.push({x : xs[i], y: vitals[i]});
      non_vitals_values.push({x: xs[i], y: non_vitals[i]});
    }

    var relevance = [ {
        key: "Vital",
        values: vitals_values
      }, {
        key: "Non-Vital",
        values: non_vitals_values
      }];
    return [staleness, relevance]
}

function registerEvent(src, dst) {
  src.dispatch.on("brush", function(evt) {
    //dst.brushExtent(evt.extent);
    //var oldTransition = dst.transitionDuration();
    //dst.transitionDuration(0); 
    //dst.dispatch.brush();
    //dst.transitionDuration(oldTransition);
  });
}


function getDocuments(e) {
  var entity  = e.id;
  d3.json('/kba/documents/' + e.id, function(error, d) {
    if (!error) {
      var data = parseData(d);
      var relevanceChart = timeChart('#relevance', 'd', data[1]);
      relevanceChart.yAxis.axisLabel('number of documents').axisLabelDistance(40);
      //relevanceChart.lines.dispatch.on('elementClick', function(e) {
        //onRelevanceClick(entity, e.point.x);
      //});
      var stalenessChart = timeChart('#staleness', ',.2f', data[0]);
      stalenessChart.yAxis.axisLabel('staleness').axisLabelDistance(40);
      //stalenessChart.interpolate("basis");
      stalenessChart.lines.dispatch.on('elementClick', function(e) {
        onClusterClick(entity, e.series.key.charAt(1), e.point.x);
      });
      //registerEvent(relevanceChart, stalenessChart);
      //registerEvent(stalenessChart, relevanceChart);
    }
  });
}

function renderModal(d) {
  wordCloud(d);
  $('#wordcloud').modal();
}

function onRelevanceClick(entity, timestamp) {
  d3.json('/kba/wordcloud/' + entity + '/' + timestamp, function(error, d) {
    if (!error) {
      renderModal(d);
    }
  });
}

function onClusterClick(entity, clusterid, timestamp) {
  d3.json('/kba/wordcloud/' + entity + '/' + clusterid + '/' + timestamp, function(error, d) {
    if (!error) {
      renderModal(d);
    }
  });
}

function timeChart(id, format, data) {
  var chart = nv.models.lineWithFocusChart();  
  chart.xAxis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000));
  });
  chart.x2Axis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000));
  });
  chart.yAxis.tickFormat(d3.format(format));
  chart.y2Axis.tickFormat(d3.format(format));

  nv.addGraph(function() {
    d3.select(id + ' svg').remove();
    d3.select(id).append('svg');
    d3.select(id + ' svg')
            .datum(data)
            .transition().duration(500)
            .call(chart);
    nv.utils.windowResize(chart.update);
    return chart;
  });
  // remove tooltips
  chart.tooltips(false);
  chart.lines.dispatch.on('elementMouseover.tooltip', null);
  chart.lines.dispatch.on('elementMouseout.tooltip', null);
  //chart.color(['#8c510a','#bf812d','#dfc27d','#f6e8c3','#c7eae5','#80cdc1','#35978f','#01665e']);
  return chart;         
}

function initTypeahead() {
    var bh = new Bloodhound({
      datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
      queryTokenizer: Bloodhound.tokenizers.whitespace,
      local: $.map(entities, function(e) { return e; })
    });
    bh.initialize();
    $('#entity .typeahead').typeahead({
      hint: true,
      highlight: true,
      minLength: 1
    },
    {
      name: 'entities',
      displayKey: 'name',
      source: bh.ttAdapter()
    })
    .on('typeahead:selected', function($e, datum){
            getDocuments(datum);
          }
        )
    .on('typeahead:autocompleted', function($e, datum){
            $('#entity .typeahead').typeahead('close');
          getDocuments(datum); 
        });
}

function wordCloud(data) {
  var words = data.map(function(d) {
                return {text: d.t, size: 10 + (d.p / 1000) * 19};
              });
  var fill = d3.scale.category20();
  d3.layout.cloud().size([300, 300])
      .words(words)
      .padding(5)
      .rotate(function() { return ~~(Math.random() * 2) * 90; })
      .font("Impact")
      .fontSize(function(d) { return d.size; })
      .on("end", draw)
      .start();

  function draw(words) {
    d3.select("#wordcloud-body svg").remove();
    d3.select("#wordcloud-body").append("svg")
        .attr("width", 300)
        .attr("height", 300)
      .append("g")
        .attr("transform", "translate(150,150)")
      .selectAll("text")
        .data(words)
      .enter().append("text")
        .style("font-size", function(d) { return d.size + "px"; })
        .style("font-family", "Impact")
        .style("fill", function(d, i) { return fill(i); })
        .attr("text-anchor", "middle")
        .attr("transform", function(d) {
          return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
        })
        .text(function(d) { return d.text; });
  }
}

function run() {
  //wordCloud();
  d3.json('/kba/entities', function(data) {
    entities = data.map(function(e, i) {
        return {id: e.id, name: e.name};
      });
    initTypeahead();
  });
}