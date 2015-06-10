var o = {
    detectLngQS: 'setLng',
    useCookie: true,
    cookieName: 'i18next',
    cookieExpirationTime: undefined,
    cookieDomain: undefined,
    
}
var _cookie = {
    create: function(name,value,minutes,domain) {
        var expires;
        if (minutes) {
            var date = new Date();
            date.setTime(date.getTime()+(minutes*60*1000));
            expires = "; expires="+date.toGMTString();
        }
        else expires = "";
        domain = (domain)? "domain="+domain+";" : "";
        document.cookie = name+"="+value+expires+";"+domain+"path=/";
    },

    read: function(name) {
        var nameEQ = name + "=";
        var ca = document.cookie.split(';');
        for(var i=0;i < ca.length;i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') c = c.substring(1,c.length);
            if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length,c.length);
        }
        return null;
    },

    remove: function(name) {
        this.create(name,"",-1);
    }
};

function detectLanguage() {
    var detectedLng;

    // get from qs
    var qsParm = [];
    if (typeof window !== 'undefined') {
        (function() {
            var query = window.location.search.substring(1);
            var parms = query.split('&');
            for (var i=0; i<parms.length; i++) {
                var pos = parms[i].indexOf('=');
                if (pos > 0) {
                    var key = parms[i].substring(0,pos);
                    var val = parms[i].substring(pos+1);
                    qsParm[key] = val;
                }
            }
        })();
        if (qsParm[o.detectLngQS]) {
            detectedLng = qsParm[o.detectLngQS];
        }
    }

    // get from cookie
    if (!detectedLng && typeof document !== 'undefined' && o.useCookie ) {
        var c = _cookie.read(o.cookieName);
        if (c) detectedLng = c;
    }

    // get from navigator
    if (!detectedLng && typeof navigator !== 'undefined') {
        detectedLng =  (navigator.language) ? navigator.language : navigator.userLanguage;
    }
    

    
    return detectedLng;
}