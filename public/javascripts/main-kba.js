
var parseDate = d3.time.format('%x')
var entities = []

function parseData(d) {
    var clusters = Math.max.apply(null, _.pluck(d, "ci"))
    var data = []
    for (i=0; i < clusters; i++) {
      data[i] = []
    }
    var vitals = []
    var non_vitals = []
    var vitals_count = 0
    var non_vitals_count = 0

    d.map(function(e,i) {
      if (e.relevance == 2) {
        vitals.push({x: e.timestamp, y: ++vitals_count})
        non_vitals.push({x: e.timestamp, y: non_vitals_count})
      } else {
        vitals.push({x: e.timestamp, y: vitals_count})
        non_vitals.push({x: e.timestamp, y: ++non_vitals_count})
      }
      e.lambdas.map(function (c, i) {
        data[c.cj-1].push({x: e.timestamp, y: c.dec})
        data[c.cj-1].push({x: e.timestamp, y: c.inc})
      });
    });

    var staleness = data.map(function(cluster, i) {
      return {
        key: "C" + (i+1),
        values: cluster
      };
    });

    var relevance = [ {
        key: "Vital",
        values: vitals
      }, {
        key: "Non-Vital",
        values: non_vitals
      }];
    return [staleness, relevance]
}

function getDocuments(e) {
  d3.json('/kba/documents/' + e.id, function(error, d) {
    if (!error) {
      var data = parseData(d);
      stalenessChart(data[0]);
      relevanceChart(data[1]);
  }
  });
}

function relevanceChart(data) {
  var chart = nv.models.stackedAreaChart();  
  chart.xAxis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000))
  });
/*  chart.x2Axis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000))
  });
*/
  chart.yAxis.tickFormat(d3.format('d'));
//  chart.y2Axis.tickFormat(d3.format('d'));

  nv.addGraph(function() {
    d3.select('#relevance svg')
            .datum(data)
            .transition().duration(500)
            .call(chart);
    nv.utils.windowResize(chart.update);
    return chart;
  });           
}

function stalenessChart(data) {
  var chart = nv.models.lineWithFocusChart();  
  chart.xAxis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000))
  });
  chart.x2Axis.tickFormat(function(d) {
    return parseDate(new Date(d * 1000))
  });
  chart.yAxis.tickFormat(d3.format(',.2f'));
  chart.y2Axis.tickFormat(d3.format(',.2f'));

  nv.addGraph(function() {
    d3.select('#staleness svg')
            .datum(data)
            .transition().duration(500)
            .call(chart);
    nv.utils.windowResize(chart.update);
    return chart;
  });           
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

function wordCloud() {
  var fill = d3.scale.category20();
  d3.layout.cloud().size([300, 300])
      .words([
        "Hello", "world", "normally", "you", "want", "more", "words",
        "than", "this"].map(function(d) {
        return {text: d, size: 10 + Math.random() * 90};
      }))
      .padding(5)
      .rotate(function() { return ~~(Math.random() * 2) * 90; })
      .font("Impact")
      .fontSize(function(d) { return d.size; })
      .on("end", draw)
      .start();

  function draw(words) {
    d3.select("body").append("svg")
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