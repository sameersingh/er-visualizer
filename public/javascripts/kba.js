/*
Rendering KBA related info (staleness and word clusters) in the ER visualizer
*/
function renderKBA(div, kba) {
    console.log(kba);
    var divStaleness = div.append("div").attr('id', 'divStaleness');
    var divClusters = div.append("div").attr('id', 'divClusters');
    renderStaleness(divStaleness, kba);
    renderClusters(divClusters, kba);
}

function renderStaleness(div, kba) {
    var margin = {top: 5, right: 5, bottom: 5, left: 5},
        width =  $("#kba").width() - margin.left - margin.right,
        height = 100 - margin.top - margin.bottom;

    var x = d3.scale.linear()
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);

    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom");

    var yAxis = d3.svg.axis()
        .scale(y)
        .orient("left");

    var area = d3.svg.area()
        .x(function(d) { return x(d.time); })
        .y0(height)
        .y1(function(d) { return y(d.value); });

    var svg = div.append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
      .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var data = kba.staleness;
    x.domain(d3.extent(kba.docs, function(d) { return d.time; }));
    y.domain([0, 1.0]); //d3.max(data, function(d) { return d.value; })]);

    svg.append("path")
      .datum(data)
      .attr("class", "area")
      .attr("d", area);

    svg.selectAll("circle")
      .data(kba.docs)
      .enter()
      .append("circle")
      .attr("cx", function(d) { return x(d.time); })
      .attr("cy", y(0.0))
      .attr("r", "3")
      .attr("fill", "#eeeeee")
      .attr("stroke", "#444444")
      .append("title")
      .text(function(d) { return d.id + "(" + new Date(d.time).toDateString() + ")"; })
}

/**
<div class="panel panel-default">
  <div class="panel-body">
    Basic panel example
  </div>
</div>
**/
function renderClusters(div, kba) {
    var num = kba.clusters.length;
    var colors = d3.scale.ordinal()
        .domain(d3.range(1,num+1))
        .range(colorbrewer.RdBu[9]);
    colors = d3.scale.category10();
    div.selectAll('div')
      .data(kba.clusters)
      .enter()
      .append("div")
      .attr("class", "panel panel-default")
      .style("color", function(d) { return colors(d.id);})
      .style("margin", "5px")
      .style("padding", "5px")
      //.style("width", "20%")
      .style("float", "left")
      .selectAll('span')
      .data(function(d) { return d.words; })
      .enter()
      .append("span")
      .html(function(w) {return w.w + '<br>';})
      .style("font-size", function(w) {return });

      //.text(function(d) { return d.words.map(function(w) { return ' '+ w.w; }); });
}