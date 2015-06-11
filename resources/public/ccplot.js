
function getQueryVariable(variable)
{
       var query = window.location.search.substring(1);
       var vars = query.split("&");
       for (var i=0;i<vars.length;i++) {
               var pair = vars[i].split("=");
               if(pair[0] == variable){return pair[1];}
       }
       return(false);
}

var urlParams = ["aes-x", "aes-x-stat", "aes-x-pos",
                 "aes-y", "aes-y-stat", "aes-y-pos",
                 "aes-fill", "aes-fill-stat", "aes-fill-pos",
                 "aes-color", "aes-color-stat", "aes-color-pos",
                ]
$(document).ready(function() {
    $("rect").click(function(e){console.log(this);});
    urlParams.forEach(function(e) {
        var p = getQueryVariable(e);

        if (p) {
            $("#" + e).val(p);
        }
    });
});


