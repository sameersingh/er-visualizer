
var parseDate = d3.time.format('%x')
var entities = []

function parseData(d) {
    var data = _.flatten(d.map(function(e, i) {
      lis = [e.lambdas.li.dec, e.lambdas.li.inc];
      return lis.map(function(li, j) {
        return {x: e.timestamp, y: li};
      });
    }));
    var datum = [{
      key: 'C',
      values: data
    }];
    return datum;    
}

function getStaleness(e) {
  d3.json('/kba/documents/' + e.id, function(error, d) {
    if (!error) {
      var data = parseData(d);
      chart = nv.models.lineWithFocusChart();  
      chart.xAxis.tickFormat(function(d) {
        return parseDate(new Date(d * 1000))
      });
      chart.x2Axis.tickFormat(function(d) {
        return parseDate(new Date(d * 1000))
      });
      chart.yAxis.tickFormat(d3.format(',.2f'));
      chart.y2Axis.tickFormat(d3.format(',.2f'));

      nv.addGraph(function() {
        d3.select('#chart svg')
                .datum(data)
                .transition().duration(500)
                .call(chart);
        nv.utils.windowResize(chart.update);
        return chart;
      });           
  }
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
            getStaleness(datum);
          }
        )
    .on('typeahead:autocompleted', function($e, datum){
            $('#entity .typeahead').typeahead('close');
          getStaleness(datum); 
        });
}

function run() {
  d3.json('/kba/entities', function(data) {
    entities = data.map(function(e, i) {
        return {id: e.id, name: e.name};
      });
    initTypeahead();
  });
}