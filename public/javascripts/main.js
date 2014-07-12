var width = 1200, //$(".canvas").parent().width(),
    height = 650; // $(".canvas").parent().height();

var color = d3.scale.category10();

var data = {
    entityArr: [],
    entityObj: {},
    links: [],
    currEnt: -1,
    currLink: -1
    };

// local vars
var svg = 0;
var link = 0, entitySel = {};
var force = 0;

function getAllLinks() {
    $.ajax({
       type: "GET",
       url: '/relation/all',
       success: function(d) {
         while(data.links.length > 0) {
           data.links.pop();
         }
         for(i=0; i<d.length; i++) {
           d[i].source = data.entityObj[d[i].sourceId];
           d[i].target = data.entityObj[d[i].targetId];
           //console.log(d[i]);
           data.links.push(d[i]);
         }
         start();
       },
       error: function(j, t, e) { console.log(e); }
    });
}

function getAllEntities() {
    $.ajax({
       type: "GET",
       url: '/entity/all',
       success: function(d) {
         while(data.entityArr.length > 0) {
           data.entityArr.pop();
         }
         for(i=0; i<d.length; i++) {
           data.entityArr.push(d[i]);
           data.entityObj[d[i].id] = d[i];
         }
         getAllLinks();
       },
       error: function(j, t, e) { console.log(e); }
    });
}

function start() {
  link = link.data(force.links(), function(d) { return d.source.id + "-" + d.target.id; });
  link.enter()
        .append("line")
        .attr("class", "relation")
        .attr("id", function(d) { return d.source.id + "-" + d.target.id; })
        .style("stroke-width", function(d) { return 3 + Math.sqrt(20*d.popularity); })
        .on("click", function(e) { selectRelation(e); d3.event.stopPropagation(); })
        .on("mouseover", function(d) {
           d3.select(this).classed("hover", true);
           showLabel(d.source);
           showLabel(d.target);
        })
        .on("mouseout", function(d) {
           d3.select(this).classed("hover", false);
           hideLabel(d.source);
           hideLabel(d.target);
        });

  link.exit().remove();

  entitySel = entitySel.data(data.entityArr, function(d) { return d.id;});
  var entitySelEnter = entitySel.enter();
  var node = entitySelEnter
                .append("g").attr("class", "node")
                .attr("id", function(e) { return e.id; })
                .on("click", function(d) {
                    if (d3.event.defaultPrevented) return; // ignore drag
                    selectEntity(d);
                    d3.event.stopPropagation();
                })
                .call(force.drag)
                .on("mouseover", showLabel)
                .on("mouseout", hideLabel);
  node
    .append("circle")
    .classed("entity", true)
    .classed("perEnt", function(d) {return d.nerTag == "PER"; })
    .classed("locEnt", function(d) {return d.nerTag == "LOC"; })
    .classed("orgEnt", function(d) {return d.nerTag == "ORG"; })
    .classed("miscEnt", function(d) {return d.nerTag == "MISC"; })
    .attr("r", function(d) { return Math.sqrt(1000*d.popularity); })
    .append("title")
    .text(function(e) { return e.name; });
  node
    .append("text")
    .attr("x", function(d) { return Math.sqrt(1000*d.popularity)+5; })
    .attr("y", function(d) { return Math.sqrt(1000*d.popularity)+5; })
    .attr("dy", ".35em")
    .style("z-index", "-10")
    .attr("visibility", "hidden")
    .text(function(e) { return e.name; });
  //entitySelEnter
  //    .append("div")
  //    .text(function(e) { return e.name; });
  entitySel.exit().remove();

  force.start();

  initTypeahead(data);
}

function showLabel(d) {
    var text = d3.select("#"+d.id).select("text");
    text.attr("visibility", "visible");
    d3.select("#"+d.id).select("circle").classed("hover", true);
}

function hideLabel(d) {
    var text = d3.select("#"+d.id).select("text");
    text.attr("visibility", "hidden");
    d3.select("#"+d.id).select("circle").classed("hover", false);
}

function run() {
    width = $(".canvas").width();
    height = $(".canvas").height(),
    svg = d3.select(".canvas").append("svg")
            .attr("width", width)
            .attr("height", height)
            .append("g")
                .call(d3.behavior.zoom().scaleExtent([0.1, 10]).on("zoom", zoom))
                .on("mousedown.zoom", null)
                .on("touchstart.zoom", null)
                .on("touchmove.zoom", null)
                .on("touchend.zoom", null)
              .append("g")
            .on("click", unselect);

    svg.append("rect")
        .attr("class", "overlay")
        .attr("width", width)
        .attr("height", height)

    entitySel = svg.selectAll(".node");
    link = svg.selectAll(".link");

    force = d3.layout.force()
              .nodes(data.entityArr)
              .links(data.links)
              .charge(-5000)
              .linkDistance(100)
              .size([width, height])
              .on("tick", tick);
    //start();
    getAllEntities();
    // end of run()
}

function zoom() {
  svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
}

function tick() {
    entitySel.attr("transform", function(d) { return "translate("+d.x + "," + d.y + ")"; });

    link.attr("x1", function(d) { return d.source.x; })
       .attr("y1", function(d) { return d.source.y; })
       .attr("x2", function(d) { return d.target.x; })
       .attr("y2", function(d) { return d.target.y; });
}

function updateEntPosition(e, cx, cy) {
    if(e.fixed) {
       var speed = 0.01;
       var incrx = cx - e.x;
       var incry = cy - e.y;
       if(incrx < -1 || incrx > 1 || incry < -1 || incry > 1) {
           //console.log("up: " + incrx + ", " + incry);
           //console.log("up b: xy(" + data.currEnt.x + ", " + data.currEnt.y + ") , pxy(" + data.currEnt.px + ", " + data.currEnt.py + ")");
           e.x = e.x + speed*incrx;
           e.y = e.y + speed*incry;
           e.px = e.x;
           e.py = e.y;
           //console.log("up a: xy(" + data.currEnt.x + ", " + data.currEnt.y + ") , pxy(" + data.currEnt.px + ", " + data.currEnt.py + ")");
           tick();
           setTimeout(function(){updateEntPosition(e, cx, cy)}, 5);
       }
    }
}

function unselect() {
    if(data.currEnt != -1) {
       data.currEnt.fixed = false;
       data.currEnt = -1;
    }
    if(data.currLink != -1) {
      data.currLink.source.fixed = false;
      data.currLink.target.fixed = false;
      data.currLink = -1;
    }
    d3.selectAll(".selected").classed("selected", false);
    //d3.select("#entityEntry .tt-input").attr("value", "");
    $("#entityEntry .tt-input").typeahead("val", "");
    hideAllBoxes();
    //force.start();
}

function selectEntity(d) {
    if(data.currEnt != d) {
        console.log("selected: " + d.id);
        unselect();
        data.currEnt = d;
        // set color and position
        d3.selectAll(".selected").classed("selected", false);
        var circle = d3.select("#"+d.id).select("circle");
        circle.classed("selected", true);
        data.currEnt.fixed = true;
        var cx = (width/2)-100;
        var cy = height/2;
        //updateEntPosition(data.currEnt, cx, cy);
        //force.start();
        // global settings
        $("#entityEntry .tt-input").typeahead("val", d.name);
        // entity info
        showAllBoxes(function(){
            getEntityCmd(d, 'info', displayEntityInfo);
            getEntityCmd(d, 'fb', displayEntityFreebase);
            getAndDisplayProvenances(d);
          });
        //force.start();
    } else {
        unselect();
    }
}

function selectRelation(d) {
    if(data.currLink != d) {
        console.log("selected: " + d.id);
        unselect();
        data.currLink = d;
        // set color and position
        d3.selectAll(".selected").classed("selected", false);
        d3.select("#"+d.source.id + "-" + d.target.id).classed("selected", true);
        d3.select("#"+d.sourceId).select("circle").classed("selected", true);
        d3.select("#"+d.targetId).select("circle").classed("selected", true);

        var cx = (width/2)-100;
        var cy = height/2;
        d.source.fixed = true;
        d.target.fixed = true;
        //updateEntPosition(d.source, cx, cy+75);
        //updateEntPosition(d.target, cx, cy-75);

        // global settings
        $("#entityEntry .tt-input").typeahead("val", d.source.name + "~>" + d.target.name);

        showAllBoxes(function(){
            getRelCmd(d, 'fb', displayRelFreebase);
            displayRelInfo(d);
            getAndDisplayProvenances(d);
        });
        //force.start();
    } else { unselect(); }
}

function hideAllBoxes() {
    d3.select(".infoBox").transition().style("opacity", "0.0")
        .each("end", function() {
            d3.select(this).style("visibility", "hidden");
            d3.select("#infoBoxText").text("");
            d3.select("#infoBoxHeading").text("");
            d3.select("#infoBoxTable").text("");
            d3.select("#infoBoxList").text("");
        });
    d3.select(".provWindow").transition().style("opacity", "0.0")
        .each("end", function() {
            d3.select(this).style("visibility", "hidden");
            d3.select("#accordion").text("");
        });
}

function showAllBoxes(onFinish) {
    d3.select(".infoBox").transition().style("opacity", "1.0")
        .each("start", function() {
            d3.select(this).style("visibility", "visible");
            d3.select("#infoBoxText").text("");
            d3.select("#infoBoxImg").attr("src", "");
            d3.select("#infoBoxHeading").text("Loading...");
            d3.select("#infoBoxTable").text("");
            d3.select("#infoBoxList").text("");
        })
        .each("end", onFinish);
    d3.select(".provWindow").transition().style("opacity", "1.0")
        .each("start", function() {
            d3.select(this).style("visibility", "visible");
            d3.select("#accordion").text("");
        });
}

function getEntityCmd(e, cmd, onFinish) {
    $.ajax({
       type: "GET",
       url: '/entity/'+cmd+'/'+e.id,
       success: function(d) { onFinish(e, d); },
       error: function(j, t, e) { console.log(e); }
    });
}

function getRelCmd(l, cmd, onFinish) {
    $.ajax({
       type: "GET",
       url: '/relation/'+cmd+'/'+l.source.id+'/'+l.target.id,
       success: function(d) { onFinish(l, d); },
       error: function(j, t, e) { console.log(e); }
    });
}

function displayEntityInfo(e, info) {
    if(data.currEnt.id == e.id) {
      console.log("disp einfo: " + e.id)
      var infoBox = d3.select("#infoBoxText");
      // d3.select(".debugBox").text(JSON.stringify(e)+JSON.stringify(info));
      infoBox.text("");
      // name
      d3.select("#infoBoxHeading").text("");
      d3.select("#infoBoxHeading")
        .append("span")
        .classed("glyphicon", true)
        .classed("glyphicon-user", e.nerTag == "PER")
        .classed("glyphicon-map-marker", e.nerTag == "LOC")
        .classed("glyphicon-briefcase", e.nerTag == "ORG")
        .classed("glyphicon-question-sign", e.nerTag == "MISC");
      d3.select("#infoBoxHeading")
        .append("span")
        .text("  " +e.name);
      // image
      d3.select("#infoBoxImg")
        .attr("src", "https://www.googleapis.com/freebase/v1/image" + info.freebaseInfo["/common/topic/image"] + "?key=AIzaSyCQVC9yA72POMg2VjiQhSJQQP1nf3ToZTs&maxwidth=100")
        .classed("img-thumbnail", true)
        .attr("width", "100px");
        //      infoBox.append("img")
        //        .attr("width", "100px")
        //        .attr("class", "img-thumbnail");
      // freebase info (as table)
      var table = d3.select("#infoBoxTable"),
          // thead = table.append("thead"),
          tbody = table.append("tbody");
      for(key in info.freebaseInfo) {
        var tr = tbody.append("tr");
        if(key == "/mid") {
          tr.append("td").text("Freebase");
          tr.append("td").append("a").attr("href", "http://www.freebase.com" + info.freebaseInfo[key]).text(e.name);
        } else {
          tr.append("td").text(key);
          tr.append("td").text(info.freebaseInfo[key]);
        }
      }
    } else {
      console.log("einfo obsolete: " + e.id + ", curr: " + data.currEnt.id);
    }
}

function displayRelInfo(l) {
    var name = l.source.name + " ~><br>" + l.target.name;
    if(data.currLink == l) {
      console.log("disp rinfo: " + name)
      var infoBox = d3.select("#infoBoxText");
      infoBox.text("Relation between " + l.source.name + " and " + l.target.name);
      // name
      d3.select("#infoBoxHeading").html(name);
      // image
      d3.select("#infoBoxImg").attr("src", "")
        .classed("img-thumbnail", false)
        .attr("width", "0px");
    } else {
      console.log("linfo obsolete: " + name + ", curr: " + data.currLink);
    }
}

function displayEntityFreebase(e, fbts) {
    if(data.currEnt.id == e.id) {
      console.log("disp efb: " + e.id);
      // console.log(fbts);
      var infoBoxList = d3.select('#infoBoxList');
      infoBoxList.text("");
      infoBoxList.selectAll("li")
        .data(fbts.types)
        .enter()
        .append("li")
        .classed('list-group-item', true)
        //.classed('prov-item', true)
        .text(function(d) {return d;});
    } else {
      console.log("efb obsolete: " + e.id + ", curr: " + data.currEnt.id);
    }
}

function displayRelFreebase(l, fbts) {
    if(data.currLink == l) {
      console.log("disp rfb: " + l.source.id + "~>" + l.target.id);
      console.log(fbts);
      var infoBoxList = d3.select('#infoBoxList');
      infoBoxList.text("");
      infoBoxList.selectAll("li")
        .data(fbts.rels)
        .enter()
        .append("li")
        .classed('list-group-item', true)
        //.classed('prov-item', true)
        .text(function(d) {return d;});
    } else {
      console.log("rfb obsolete: " + l + ", curr: " + data.currLink);
    }
}

function getAndDisplayProvenances(d) {
    var isEntity = true;
    if(d == data.currLink) {
        isEntity = false;
    }
    var accordian = d3.select('#accordion');
    // add all text panel
    var textPanel = accordian
                        .append("div")
                        .attr("class", "panel panel-default");
    // heading
    textPanel.append("div")
             .attr("class", "panel-heading")
             .append("h4")
             .attr("class", "panel-title")
             .append("a")
             .attr("data-toggle", "collapse")
             .attr("data-parent", "#accordian")
             .attr("href", "#allTextPanel")
             .text("All Sentences");
    // body
    textPanel.append("div")
             .attr("class", "panel-collapse collapse")
             .attr("id", "allTextPanel")
             .append("ul")
             .attr("class", "list-group provListGroup")
             .attr("id", "allTextList")
             .text("Loading...");
    if(isEntity) {
      getEntityCmd(d, 'text', displayEntityText);
      getEntityCmd(d, 'types', displayTypeProvs);
    } else {
      getRelCmd(d, 'text', displayEntityText);
      getRelCmd(d, 'types', displayTypeProvs);
    }
}

function displayEntityText(e, txt) {
    if(data.currEnt == e || data.currLink == e ) {
      // console.log("disp etxt: " + e.id);
      var allTextList = d3.select('#allTextList');
      allTextList.text("");
      allTextList.selectAll("li")
        .data(txt.provenances)
        .enter()
        .append("li")
        .classed('list-group-item', true)
        .classed('prov-item', true)
        .text("Loading...")
        .each(function(d) { displayProv(d, d3.select(this)) });
    } else {
      console.log("etxt obsolete: " + e.id + ", curr: " + data.currEnt.id);
    }
}

function displayTypeProvs(e, types) {
    if(data.currEnt == e || data.currLink == e) {
      console.log("disp etPs: " + e.id);
      var pairs = types.map(function(t) {return { src: e, type: t};})
      // console.log(types);
      // console.log(pairs);
      var accordion = d3.select('#accordion');
      // add all text panel
      var textPanel = accordion
                      .selectAll(".typeProv")
                      .data(pairs)
                      .enter()
                      .append("div")
                      .attr("class", "panel panel-default typeProv");
      // heading
      textPanel.append("div")
               .attr("class", "panel-heading")
               .append("h4")
               .attr("class", "panel-title")
               .append("a")
               .attr("data-toggle", "collapse")
               .attr("data-parent", "#accordian")
               .attr("href", function(d) {return '#'+d.type;})
               .text(function(d) {return d.type;});
      // body
      textPanel.append("div")
               .attr("class", "panel-collapse collapse")
               .attr("id", function(d) {return d.type;})
               .append("ul")
               .attr("class", "list-group provListGroup")
               .attr("id", function(d) {return d.type+'List';})
               .each(function(p) {
                 getAndDisplayTypeProv(p.src, p.type, d3.select(this));
               });
    } else {
      console.log("etPs obsolete: " + e.id + ", curr: " + data.currEnt.id);
    }
}

function getAndDisplayTypeProv(e, type, dom) {
    if(e == data.currEnt) {
        $.ajax({
           type: "GET",
           url: '/entity/typeprov/'+e.id+'/'+type,
           success: function(d) {
             displayTypeProv(e, type, dom, d);
           },
           error: function(j, t, e) { console.log(e); }
        });
    } else {
        $.ajax({
           type: "GET",
           url: '/relation/typeprov/'+e.source.id+'/'+e.target.id+'/'+type,
           success: function(d) {
             console.log(d);
             displayTypeProv(e, type, dom, d);
           },
           error: function(j, t, e) { console.log(e); }
        });
    }
}

function displayTypeProv(e, type, dom, tp) {
    if(data.currEnt == e || data.currLink == e) {
      console.log("disp etp: " + e + ", "+ type);
      //console.log(tp);
      var typeProvList = dom;
      typeProvList.text("");
      typeProvList.selectAll("li")
        .data(tp.provenances)
        .enter()
        .append("li")
        .classed('list-group-item', true)
        .classed('prov-item', true)
        .text("Loading...")
        .each(function(p) { displayProv(p, d3.select(this)) });
    } else {
      console.log("etxt obsolete: " + e + ", curr: " + data.currEnt.id);
    }
}


function displayProv(prov, dom) {
  //dom.text(JSON.stringify(prov));
  $.ajax({
     type: "GET",
     url: '/docs/sentence/'+prov.docId+'/'+prov.sentId,
     success: function(d) {
       dom.text("");
       // docid
       dom.append("a")
         .attr("href", "/docs/doc/"+d.docId)
         .attr("target", "_blank")
         .text(d.docId)
         .classed("docId", true);
       var sent = d.string;
       // dom.append("span")
       //  .text(sent + JSON.stringify(prov.tokPos));
       prov.tokPos.sort(function(a,b) {return a[0]-b[0]});
       var currIndex = 0;
       for(i=0; i<prov.tokPos.length; i+=1) {
         // string
         dom.append("span")
           .text(sent.slice(currIndex, prov.tokPos[i][0]));
         // mention
         dom.append("mark")
           //.classed("argument", true)
           .text(sent.slice(prov.tokPos[i][0], prov.tokPos[i][1]));
         currIndex = prov.tokPos[i][1];
       }
       // last part of sent
       dom.append("span")
           .text(sent.slice(currIndex, sent.length));
     },
     error: function(j, t, e) { console.log(e); }
  });
}

function initTypeahead(data) {
    // constructs the suggestion engine
    var bh = new Bloodhound({
      datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
      queryTokenizer: Bloodhound.tokenizers.whitespace,
      // `states` is an array of state names defined in "The Basics"
      local: $.map(data.entityArr, function(e) { return e; })
    });

    // kicks off the loading/processing of `local` and `prefetch`
    bh.initialize();

    $('#entityEntry .typeahead').typeahead({
      hint: true,
      highlight: true,
      minLength: 1
    },
    {
      name: 'states',
      displayKey: 'name',
      // `ttAdapter` wraps the suggestion engine in an adapter that
      // is compatible with the typeahead jQuery plugin
      source: bh.ttAdapter()
    })
    .on('typeahead:selected', function($e, datum){
            selectEntity(datum);
          }
        )
    .on('typeahead:autocompleted', function($e, datum){
                $('#entityEntry .typeahead').typeahead('close');
                selectEntity(datum);
              }
            );
}