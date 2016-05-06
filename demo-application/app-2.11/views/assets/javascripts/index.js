
var map;
var markers = [];
var geohashCells = [];
var factor = 1.0;

function clearMarkers() {
    while(markers.length){
        markers.pop().setMap(null);
    }
}

function addMarker(lat, lon, title, icon) {
    markers.push(new google.maps.Marker({
        position: new google.maps.LatLng(lat, lon),
        map: map,
        title: title,
        icon: icon,
        shadow: null
    }));
}

function clearGeohashCells() {
    while(geohashCells.length){
        geohashCells.pop().setMap(null);
    }
}

function addGeohashCell(geohashCell) {
    geohashCells.push(new google.maps.Rectangle({
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.35,
        map: map,
        bounds: new google.maps.LatLngBounds(
            new google.maps.LatLng(geohashCell.top_left.latitude, geohashCell.top_left.longitude),
            new google.maps.LatLng(geohashCell.bottom_right.latitude, geohashCell.bottom_right.longitude))
    }));
}

function fetchFacets() {
    var ne = map.getBounds().getNorthEast();
    var sw = map.getBounds().getSouthWest();
    var center = map.getCenter();

// r = radius of the earth in statute miles
    var r = 6356000.755;

// Convert lat or lng from decimal degrees into radians (divide by 57.2958)
    var lat1 = center.lat() / 57.2958;
    var lon1 = center.lng() / 57.2958;
    var lat2 = ne.lat() / 57.2958;
    var lon2 = ne.lng() / 57.2958;

// distance = circle radius from center to Northeast corner of bounds
// var dis = r * Math.acos(Math.sin(lat1) * Math.sin(lat2) +
// Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
    var dis = 4000

// alert(map.getRadius());
    console.log("querying with factor " + factor);
    $.ajax({
            url: "http://120.26.154.166/api/v1/statuses/public_hotspots?latitude="+center.lat()+"&longitude="+center.lng()+"&radius="+dis+"&limit=20&offset=0&dateBegin=1433649600000&dateEnd=1434859200000"
// ,
// type: "GET",
// data: JSON.stringify({
// query: {
// filtered: {
// query: {
// match_all: {}
// },
// filter: {
// geo_bounding_box: {
// location: {
// top_left: {
// "lat": ne.lat(),
// "lon": sw.lng()
// },
// bottom_right: {
// "lat": sw.lat(),
// "lon": ne.lng()
// }
// }
// }
// }
// }
// },
// facets: {
// places: {
// geohash: {
// field: "location",
// factor: factor,
// show_geohash_cell: true
// }
// }
// }
// }),
// dataType: "json"
        }
    )
        .done(function(data){
            clearMarkers();
            clearGeohashCells();

            var clusters = data.content;
            console.log('received ' + clusters.length + ' clusters');
            for (var i = 0; i < clusters.length; i++) {
                console.log('received posts' + clusters[i].size);

                addMarker(
                    clusters[i].center.latitude,
                    clusters[i].center.longitude,
                        clusters[i].size == 1?
                        "single item @" + clusters[i].center.latitude + ", " + clusters[i].center.latitude:
                        "cluster (" + clusters[i].size + ") @" + clusters[i].center.latitude + ", " + clusters[i].center.longitude,
                    groupIcon(clusters[i].size)
                );
                addGeohashCell(clusters[i]);

            }
        });
}

function groupIcon(groupSize) {
    return groupSize > 1?
        'https://chart.googleapis.com/chart?chst=d_map_spin&chld=1.0|0|FF8429|16|b|' + groupSize:
        'https://chart.googleapis.com/chart?chst=d_map_spin&chld=0.5|0|FF8429|16|b|';
}


function initialize(divId){

    initMap(divId);

}

function initMap(divId){
    var mapOptions = {
        zoom: 8,
        center: new google.maps.LatLng(36.2673, -115.0224),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    map = new google.maps.Map(document.getElementById(divId), mapOptions);

    google.maps.event.addDomListener(window, 'resize', function(){ fetchFacets(); } );
    google.maps.event.addListener(map, 'dragend', function(){ fetchFacets(); } );
    google.maps.event.addListener(map, 'zoom_changed', function(){ fetchFacets(); } );

}
