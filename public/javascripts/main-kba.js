

function test() {
    $.ajax({
       type: "GET",
       url: '/document',
       success: function(d) {
        console.log(d);
       },
       error: function(j, t, e) { console.log(e); }
    });
}


function run() {
  test();
}