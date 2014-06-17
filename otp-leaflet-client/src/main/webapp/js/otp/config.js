otp.config = {
    debug: false,

    locale: otp.locale.Slovenian,
    locale_short: 'sl',

    //Add all locales you want to see in your frontend langzuage chooser
    //code must be the same as locale name in js/locale withoud .js part
    //name must be name of the language in that language
    active_locales : [
        { 
            code: 'en',
            name: 'English'
        },
        {
            code:'de',
            name: 'Deutsch'
        }
    ],

    languageChooser : function() {
        var str = "<ul>";
        var localesLength = otp.config.active_locales.length;
        var param_name = i18n.options.detectLngQS;
        for (var i = 0; i < localesLength; i++) {
            var current_locale = otp.config.active_locales[i];
            var url_param = {};
            url_param[param_name] = current_locale.code;
            str += '<li><a href="?' + $.param(url_param) + '">' + current_locale.name + ' (' + current_locale.code + ')</a></li>';
        }
        str += "</ul>";
        return str;
    },


    /**
     * The OTP web service locations
     */
    hostname : "",
    //municoderHostname : "http://localhost:8080",
    //datastoreUrl : 'http://localhost:9000',
    restService : "otp-rest-servlet",
    timeOffset : 0,


    /**
     * Base layers: the base map tile layers available for use by all modules.
     * Expressed as an array of objects, where each object has the following 
     * fields:
     *   - name: <string> a unique name for this layer, used for both display
     *       and internal reference purposes
     *   - tileUrl: <string> the map tile service address (typically of the
     *       format 'http://{s}.yourdomain.com/.../{z}/{x}/{y}.png')
     *   - attribution: <string> the attribution text for the map tile data
     *   - [subdomains]: <array of strings> a list of tileUrl subdomains, if
     *       applicable
     *       
     */
     
    baseLayers: [
        {
            name: 'MapQuest OSM',
            tileUrl: 'http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png',
            subdomains : ['otile1','otile2','otile3','otile4'],
            attribution : 'Data, imagery and map information provided by <a href="http://open.mapquest.com" target="_blank">MapQuest</a>, <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors.'
        },
        {
            name: 'MapQuest Aerial',
            tileUrl: 'http://{s}.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.png',
            subdomains : ['otile1','otile2','otile3','otile4'],
            attribution : 'Data, imagery and map information provided by <a href="http://open.mapquest.com" target="_blank">MapQuest</a>, <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors.'
        },           
    ],
    

    /**
     * Map start location and zoom settings: by default, the client uses the
     * OTP metadata API call to center and zoom the map. The following
     * properties, when set, override that behavioir.
     */
     
    // initLatLng : new L.LatLng(<lat>, <lng>),
    // initZoom : 14,
    // minZoom : 10,
    // maxZoom : 20,
    
    /* Whether the map should be moved to contain the full itinerary when a result is received. */
    zoomToFitResults    : false,

    /**
     * Site name / description / branding display options
     */

    siteName            : "Daljinko<sup>Beta</sup>",
    siteDescription     : "MARPROM Daljinko",
    logoGraphic         : 'images/otp_logo_darkbg_40px.png',
    bikeshareName       : "BCikel",

    showLogo            : true,
    showTitle           : true,
    showModuleSelector  : true,
    metric              : true,


    /**
     * Modules: a list of the client modules to be loaded at startup. Expressed
     * as an array of objects, where each object has the following fields:
     *   - id: <string> a unique identifier for this module
     *   - className: <string> the name of the main class for this module; class
     *       must extend otp.modules.Module
     *   - [defaultBaseLayer] : <string> the name of the map tile base layer to
     *       used by default for this module
     *   - [isDefault]: <boolean> whether this module is shown by default;
     *       should only be 'true' for one module
     */
    
    modules : [
        {
            id : 'planner',
            className : 'otp.modules.multimodal.MultimodalPlannerModule',
            defaultBaseLayer : 'MapQuest OSM',
            isDefault: true
        },
	{
	    id : 'bikeshare',
	    className : 'otp.modules.bikeshare.BikeShareModule',
	},
        /*{
            id : 'analyst',
            className : 'otp.modules.analyst.AnalystModule',
        },
        */

    ],
    
    
    /**
     * Geocoders: a list of supported geocoding services available for use in
     * address resolution. Expressed as an array of objects, where each object
     * has the following fields:
     *   - name: <string> the name of the service to be displayed to the user
     *   - className: <string> the name of the class that implements this service
     *   - url: <string> the location of the service's API endpoint
     *   - addressParam: <string> the name of the API parameter used to pass in
     *       the user-specifed address string
     */

    geocoders : [
       /* {
        'name': 'geocoder',
        'className': 'otp.core.Geocoder',
        'url': 'http://localhost:8080/otp-geocoder/geocode',
        'addressParam': 'address'
        }*/
    ],

    
    /**
     * Info Widgets: a list of the non-module-specific "information widgets"
     * that can be accessed from the top bar of the client display. Expressed as
     * an array of objects, where each object has the following fields:
     *   - content: <string> the HTML content of the widget
     *   - [title]: <string> the title of the widget
     *   - [cssClass]: <string> the name of a CSS class to apply to the widget.
     *        If not specified, the default styling is used.
     */


    infoWidgets: [
        {
            title: 'O strani',
            content: '<p>Beta verzija načrtovalnika poti za Maribor.</p>' +
            '<p>Načrtovanje poti je trenutno mogoče samo v okolici Maribora (zaradi male zmogljivosti strežnika).</p>' +
            '<p>Trenutno so vključeni vozni redi Marproma in lokacije postaj za izposojo koles BCikel</p>' +
            '<p>Niso dodane vse vožnje avtobusov in nekateri stojijo na drugih postajah kot v podatkih:</p>' +
            '<p>Manjka vožnja 20ke preko Zrkovc. Kje je postaja Qlandia, kjer stoji 18-ka v soboto? Ali avtobus 7 stoji na Orožnovi v soboto namesto na Strossmayerjevi. Problem je tudi pri linijah 16, 20 in Krožna 1 ki naj bi stale na TVD Osojnikova, čeprav je to s poti. Predvidevam da stojijo na TVD Čufarjeva. Linija 3 bi naj stala na Osojnikova za Osojnikova TVD Partizan, kar predvidevam da je napaka. <strong>Vsi ostali postanki bi morali ustrazati dejanskim stanjem</strong></p>' +
            '<p>Temelji na <a href="http://www.opentripplanner.org/">OpenTripPlanner</a>-ju</p>',
            //cssClass: 'otp-contactWidget',
        },
        /*{
            title: 'Kontakt',
            content: '<p>Comments? Contact us at...</p>'
        },
        //Enable this if you want to show frontend language chooser
        {
            title: '<img src="/images/language_icon.svg" onerror="this.onerror=\'\';this.src=\'/images/language_icon.png\'" width="30px" height="30px"/>', 
            languages: true
        } */
    ],
    
    
    /**
     * Support for the "AddThis" display for sharing to social media sites, etc.
     */
     
    showAddThis     : false,
    //addThisPubId    : 'your-addthis-id',
    //addThisTitle    : 'Your title for AddThis sharing messages',


    /**
     * Formats to use for date and time displays, expressed as ISO-8601 strings.
     */    
     
    timeFormat  : "h:mma",
    dateFormat  : "MMM Do YYYY"

};
var options = {
	resGetPath: 'js/otp/locale/__lng__.json',
	fallbackLng: 'en',
        nsseparator: ';;', //Fixes problem when : is in translation text
        keyseparator: '_|_',
	preload: [otp.config.locale_short],
        //TODO: Language choosing works only with this disabled
        lng: otp.config.locale_short,
        /*postProcess: 'add_nekaj', //Adds | around every string that is translated*/
        /*shortcutFunction: 'sprintf',*/
        /*postProcess: 'sprintf',*/
	debug: true,
	getAsync: false, //TODO: make async
	fallbackOnEmpty: true,
};
var _tr = null; //key
var ngettext = null; // singular, plural, value
var pgettext = null; // context, key
var npgettext = null; // context, singular, plural, value

i18n.addPostProcessor('add_nekaj', function(val, key, opts) {
    return "|"+val+"|";
});

i18n.init(options, function(t) {
    console.log("loaded");
    //Accepts Key, value or key, value1 ... valuen
    //Key is string to be translated
    //Value is used for sprintf parameter values
    //http://www.diveintojavascript.com/projects/javascript-sprintf
    //Value is optional and can be one parameter as javascript object if key
    //has named parameters
    //Or can be multiple parameters if used as positional sprintf parameters
    _tr = function() {
        var arg_length = arguments.length;
        //Only key
        if (arg_length == 1) {
            key = arguments[0];
            return t(key); 
        //key with sprintf values
        } else if (arg_length > 1) {
            key = arguments[0];
            values = [];
            for(var i = 1; i < arg_length; i++) {
                values.push(arguments[i]);
            }
            return t(key, {postProcess: 'sprintf', sprintf: values}); 
        } else {
            console.error("_tr function doesn't have an argument");
            return "";
        }
    };
    ngettext = function(singular, plural, value) {
        return t(singular, {count: value, postProcess: 'sprintf', sprintf: [value]});
    };
    pgettext = function(context, key) {
        return t(key, {context: context});
    };
    npgettext = function(context, singular, plural, value) {
        return t(singular, {context: context,
                 count: value,
                 postProcess: 'sprintf',
                 sprintf: [value]});
    };

});

otp.config.modes = {
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "TRANSIT,WALK"        : _tr("Transit"), 
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "BUSISH,WALK"         : _tr("Bus Only"), 
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        /*"TRAINISH,WALK"       : _tr("Rail Only"), */
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "BICYCLE"             : _tr('Bicycle Only'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "TRANSIT,BICYCLE"     : _tr("Bicycle &amp; Transit"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "WALK"                : _tr('Walk Only'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        "CAR"                 : _tr('Drive Only'),
        //uncomment only if bike rental exists in a map
        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        'WALK,BICYCLE'        :_tr('Rented Bicycle'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel
    //Options widgets)
        'TRANSIT,WALK,BICYCLE': _tr('Transit & Rented Bicycle')
    };
