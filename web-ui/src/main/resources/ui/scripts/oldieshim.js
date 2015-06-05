! function(t, e) {
    "use strict";
    "function" == typeof define && define.amd ? define(e) : "object" == typeof exports ? module.exports = e() : t.returnExports = e()
}(this, function() {
    function t(t) {
        var e = typeof t;
        return null === t || "undefined" === e || "boolean" === e || "number" === e || "string" === e
    }
    var e, r = Array.prototype,
        n = Object.prototype,
        o = Function.prototype,
        i = String.prototype,
        a = Number.prototype,
        l = r.slice,
        u = r.splice,
        c = r.push,
        s = r.unshift,
        f = o.call,
        p = n.toString,
        h = Array.isArray || function(t) {
            return "[object Array]" === p.call(t)
        },
        g = "function" == typeof Symbol && "symbol" == typeof Symbol.toStringTag,
        y = Function.prototype.toString,
        d = function(t) {
            try {
                return y.call(t), !0
            } catch (e) {
                return !1
            }
        },
        v = "[object Function]",
        b = "[object GeneratorFunction]";
    e = function(t) {
        if ("function" != typeof t) return !1;
        if (g) return d(t);
        var e = p.call(t);
        return e === v || e === b
    };
    var m, w = RegExp.prototype.exec,
        T = function(t) {
            try {
                return w.call(t), !0
            } catch (e) {
                return !1
            }
        },
        O = "[object RegExp]";
    m = function(t) {
        return "object" != typeof t ? !1 : g ? T(t) : p.call(t) === O
    };
    var j, S = String.prototype.valueOf,
        x = function(t) {
            try {
                return S.call(t), !0
            } catch (e) {
                return !1
            }
        },
        C = "[object String]";
    j = function(t) {
        return "string" == typeof t ? !0 : "object" != typeof t ? !1 : g ? x(t) : p.call(t) === C
    };
    var N = function(t) {
            var r = p.call(t),
                n = "[object Arguments]" === r;
            return n || (n = !h(t) && null !== t && "object" == typeof t && "number" == typeof t.length && t.length >= 0 && e(t.callee)), n
        },
        E = function(t) {
            var e, r = Object.defineProperty && function() {
                try {
                    return Object.defineProperty({}, "x", {}), !0
                } catch (t) {
                    return !1
                }
            }();
            return e = r ? function(t, e, r, n) {
                    !n && e in t || Object.defineProperty(t, e, {
                        configurable: !0,
                        enumerable: !1,
                        writable: !0,
                        value: r
                    })
                } : function(t, e, r, n) {
                    !n && e in t || (t[e] = r)
                },
                function(r, n, o) {
                    for (var i in n) t.call(n, i) && e(r, i, n[i], o)
                }
        }(n.hasOwnProperty),
        A = {
            ToInteger: function(t) {
                var e = +t;
                return e !== e ? e = 0 : 0 !== e && e !== 1 / 0 && e !== -(1 / 0) && (e = (e > 0 || -1) * Math.floor(Math.abs(e))), e
            },
            ToPrimitive: function(r) {
                var n, o, i;
                if (t(r)) return r;
                if (o = r.valueOf, e(o) && (n = o.call(r), t(n))) return n;
                if (i = r.toString, e(i) && (n = i.call(r), t(n))) return n;
                throw new TypeError
            },
            ToObject: function(t) {
                if (null == t) throw new TypeError("can't convert " + t + " to object");
                return Object(t)
            },
            ToUint32: function(t) {
                return t >>> 0
            }
        },
        I = function() {};
    E(o, {
        bind: function(t) {
            var r = this;
            if (!e(r)) throw new TypeError("Function.prototype.bind called on incompatible " + r);
            for (var n, o = l.call(arguments, 1), i = function() {
                    if (this instanceof n) {
                        var e = r.apply(this, o.concat(l.call(arguments)));
                        return Object(e) === e ? e : this
                    }
                    return r.apply(t, o.concat(l.call(arguments)))
                }, a = Math.max(0, r.length - o.length), u = [], c = 0; a > c; c++) u.push("$" + c);
            return n = Function("binder", "return function (" + u.join(",") + "){ return binder.apply(this, arguments); }")(i), r.prototype && (I.prototype = r.prototype, n.prototype = new I, I.prototype = null), n
        }
    });
    var M = f.bind(n.hasOwnProperty),
        D = function() {
            var t = [1, 2],
                e = t.splice();
            return 2 === t.length && h(e) && 0 === e.length
        }();
    E(r, {
        splice: function() {
            return 0 === arguments.length ? [] : u.apply(this, arguments)
        }
    }, !D);
    var U = function() {
        var t = {};
        return r.splice.call(t, 0, 0, 1), 1 === t.length
    }();
    E(r, {
        splice: function(t, e) {
            if (0 === arguments.length) return [];
            var r = arguments;
            return this.length = Math.max(A.ToInteger(this.length), 0), arguments.length > 0 && "number" != typeof e && (r = l.call(arguments), r.length < 2 ? r.push(this.length - t) : r[1] = A.ToInteger(e)), u.apply(this, r)
        }
    }, !U);
    var _ = 1 !== [].unshift(0);
    E(r, {
        unshift: function() {
            return s.apply(this, arguments), this.length
        }
    }, _), E(Array, {
        isArray: h
    });
    var F = Object("a"),
        J = "a" !== F[0] || !(0 in F),
        k = function(t) {
            var e = !0,
                r = !0;
            return t && (t.call("foo", function(t, r, n) {
                "object" != typeof n && (e = !1)
            }), t.call([1], function() {
                "use strict";
                r = "string" == typeof this
            }, "x")), !!t && e && r
        };
    E(r, {
        forEach: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = arguments[1],
                i = -1,
                a = n.length >>> 0;
            if (!e(t)) throw new TypeError;
            for (; ++i < a;) i in n && t.call(o, n[i], i, r)
        }
    }, !k(r.forEach)), E(r, {
        map: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = n.length >>> 0,
                i = Array(o),
                a = arguments[1];
            if (!e(t)) throw new TypeError(t + " is not a function");
            for (var l = 0; o > l; l++) l in n && (i[l] = t.call(a, n[l], l, r));
            return i
        }
    }, !k(r.map)), E(r, {
        filter: function(t) {
            var r, n = A.ToObject(this),
                o = J && j(this) ? this.split("") : n,
                i = o.length >>> 0,
                a = [],
                l = arguments[1];
            if (!e(t)) throw new TypeError(t + " is not a function");
            for (var u = 0; i > u; u++) u in o && (r = o[u], t.call(l, r, u, n) && a.push(r));
            return a
        }
    }, !k(r.filter)), E(r, {
        every: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = n.length >>> 0,
                i = arguments[1];
            if (!e(t)) throw new TypeError(t + " is not a function");
            for (var a = 0; o > a; a++)
                if (a in n && !t.call(i, n[a], a, r)) return !1;
            return !0
        }
    }, !k(r.every)), E(r, {
        some: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = n.length >>> 0,
                i = arguments[1];
            if (!e(t)) throw new TypeError(t + " is not a function");
            for (var a = 0; o > a; a++)
                if (a in n && t.call(i, n[a], a, r)) return !0;
            return !1
        }
    }, !k(r.some));
    var P = !1;
    r.reduce && (P = "object" == typeof r.reduce.call("es5", function(t, e, r, n) {
        return n
    })), E(r, {
        reduce: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = n.length >>> 0;
            if (!e(t)) throw new TypeError(t + " is not a function");
            if (!o && 1 === arguments.length) throw new TypeError("reduce of empty array with no initial value");
            var i, a = 0;
            if (arguments.length >= 2) i = arguments[1];
            else
                for (;;) {
                    if (a in n) {
                        i = n[a++];
                        break
                    }
                    if (++a >= o) throw new TypeError("reduce of empty array with no initial value")
                }
            for (; o > a; a++) a in n && (i = t.call(void 0, i, n[a], a, r));
            return i
        }
    }, !P);
    var R = !1;
    r.reduceRight && (R = "object" == typeof r.reduceRight.call("es5", function(t, e, r, n) {
        return n
    })), E(r, {
        reduceRight: function(t) {
            var r = A.ToObject(this),
                n = J && j(this) ? this.split("") : r,
                o = n.length >>> 0;
            if (!e(t)) throw new TypeError(t + " is not a function");
            if (!o && 1 === arguments.length) throw new TypeError("reduceRight of empty array with no initial value");
            var i, a = o - 1;
            if (arguments.length >= 2) i = arguments[1];
            else
                for (;;) {
                    if (a in n) {
                        i = n[a--];
                        break
                    }
                    if (--a < 0) throw new TypeError("reduceRight of empty array with no initial value")
                }
            if (0 > a) return i;
            do a in n && (i = t.call(void 0, i, n[a], a, r)); while (a--);
            return i
        }
    }, !R);
    var Z = Array.prototype.indexOf && -1 !== [0, 1].indexOf(1, 2);
    E(r, {
        indexOf: function(t) {
            var e = J && j(this) ? this.split("") : A.ToObject(this),
                r = e.length >>> 0;
            if (!r) return -1;
            var n = 0;
            for (arguments.length > 1 && (n = A.ToInteger(arguments[1])), n = n >= 0 ? n : Math.max(0, r + n); r > n; n++)
                if (n in e && e[n] === t) return n;
            return -1
        }
    }, Z);
    var $ = Array.prototype.lastIndexOf && -1 !== [0, 1].lastIndexOf(0, -3);
    E(r, {
        lastIndexOf: function(t) {
            var e = J && j(this) ? this.split("") : A.ToObject(this),
                r = e.length >>> 0;
            if (!r) return -1;
            var n = r - 1;
            for (arguments.length > 1 && (n = Math.min(n, A.ToInteger(arguments[1]))), n = n >= 0 ? n : r - Math.abs(n); n >= 0; n--)
                if (n in e && t === e[n]) return n;
            return -1
        }
    }, $);
    var z = !{
            toString: null
        }.propertyIsEnumerable("toString"),
        H = function() {}.propertyIsEnumerable("prototype"),
        Y = !M("x", "0"),
        B = ["toString", "toLocaleString", "valueOf", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable", "constructor"],
        L = B.length;
    E(Object, {
        keys: function(t) {
            var r = e(t),
                n = N(t),
                o = null !== t && "object" == typeof t,
                i = o && j(t);
            if (!o && !r && !n) throw new TypeError("Object.keys called on a non-object");
            var a = [],
                l = H && r;
            if (i && Y || n)
                for (var u = 0; u < t.length; ++u) a.push(String(u));
            if (!n)
                for (var c in t) l && "prototype" === c || !M(t, c) || a.push(String(c));
            if (z)
                for (var s = t.constructor, f = s && s.prototype === t, p = 0; L > p; p++) {
                    var h = B[p];
                    f && "constructor" === h || !M(t, h) || a.push(h)
                }
            return a
        }
    });
    var G = Object.keys && function() {
            return 2 === Object.keys(arguments).length
        }(1, 2),
        X = Object.keys;
    E(Object, {
        keys: function(t) {
            return X(N(t) ? r.slice.call(t) : t)
        }
    }, !G);
    var q = -621987552e5,
        K = "-000001",
        Q = Date.prototype.toISOString && -1 === new Date(q).toISOString().indexOf(K);
    E(Date.prototype, {
        toISOString: function() {
            var t, e, r, n, o;
            if (!isFinite(this)) throw new RangeError("Date.prototype.toISOString called on non-finite value.");
            for (n = this.getUTCFullYear(), o = this.getUTCMonth(), n += Math.floor(o / 12), o = (o % 12 + 12) % 12, t = [o + 1, this.getUTCDate(), this.getUTCHours(), this.getUTCMinutes(), this.getUTCSeconds()], n = (0 > n ? "-" : n > 9999 ? "+" : "") + ("00000" + Math.abs(n)).slice(n >= 0 && 9999 >= n ? -4 : -6), e = t.length; e--;) r = t[e], 10 > r && (t[e] = "0" + r);
            return n + "-" + t.slice(0, 2).join("-") + "T" + t.slice(2).join(":") + "." + ("000" + this.getUTCMilliseconds()).slice(-3) + "Z"
        }
    }, Q);
    var V = !1;
    try {
        V = Date.prototype.toJSON && null === new Date(0 / 0).toJSON() && -1 !== new Date(q).toJSON().indexOf(K) && Date.prototype.toJSON.call({
            toISOString: function() {
                return !0
            }
        })
    } catch (W) {}
    V || (Date.prototype.toJSON = function() {
        var t, e = Object(this),
            r = A.ToPrimitive(e);
        if ("number" == typeof r && !isFinite(r)) return null;
        if (t = e.toISOString, "function" != typeof t) throw new TypeError("toISOString property is not callable");
        return t.call(e)
    });
    var te = 1e15 === Date.parse("+033658-09-27T01:46:40.000Z"),
        ee = !isNaN(Date.parse("2012-04-04T24:00:00.500Z")) || !isNaN(Date.parse("2012-11-31T23:59:59.000Z")),
        re = isNaN(Date.parse("2000-01-01T00:00:00.000Z"));
    (!Date.parse || re || ee || !te) && (Date = function(t) {
        function e(r, n, o, i, a, l, u) {
            var c = arguments.length;
            if (this instanceof t) {
                var s = 1 === c && String(r) === r ? new t(e.parse(r)) : c >= 7 ? new t(r, n, o, i, a, l, u) : c >= 6 ? new t(r, n, o, i, a, l) : c >= 5 ? new t(r, n, o, i, a) : c >= 4 ? new t(r, n, o, i) : c >= 3 ? new t(r, n, o) : c >= 2 ? new t(r, n) : c >= 1 ? new t(r) : new t;
                return s.constructor = e, s
            }
            return t.apply(this, arguments)
        }

        function r(t, e) {
            var r = e > 1 ? 1 : 0;
            return i[e] + Math.floor((t - 1969 + r) / 4) - Math.floor((t - 1901 + r) / 100) + Math.floor((t - 1601 + r) / 400) + 365 * (t - 1970)
        }

        function n(e) {
            return Number(new t(1970, 0, 1, 0, 0, 0, e))
        }
        var o = new RegExp("^(\\d{4}|[+-]\\d{6})(?:-(\\d{2})(?:-(\\d{2})(?:T(\\d{2}):(\\d{2})(?::(\\d{2})(?:(\\.\\d{1,}))?)?(Z|(?:([-+])(\\d{2}):(\\d{2})))?)?)?)?$"),
            i = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365];
        for (var a in t) e[a] = t[a];
        return e.now = t.now, e.UTC = t.UTC, e.prototype = t.prototype, e.prototype.constructor = e, e.parse = function(e) {
            var i = o.exec(e);
            if (i) {
                var a, l = Number(i[1]),
                    u = Number(i[2] || 1) - 1,
                    c = Number(i[3] || 1) - 1,
                    s = Number(i[4] || 0),
                    f = Number(i[5] || 0),
                    p = Number(i[6] || 0),
                    h = Math.floor(1e3 * Number(i[7] || 0)),
                    g = Boolean(i[4] && !i[8]),
                    y = "-" === i[9] ? 1 : -1,
                    d = Number(i[10] || 0),
                    v = Number(i[11] || 0);
                return (f > 0 || p > 0 || h > 0 ? 24 : 25) > s && 60 > f && 60 > p && 1e3 > h && u > -1 && 12 > u && 24 > d && 60 > v && c > -1 && c < r(l, u + 1) - r(l, u) && (a = 60 * (24 * (r(l, u) + c) + s + d * y), a = 1e3 * (60 * (a + f + v * y) + p) + h, g && (a = n(a)), a >= -864e13 && 864e13 >= a) ? a : 0 / 0
            }
            return t.parse.apply(this, arguments)
        }, e
    }(Date)), Date.now || (Date.now = function() {
        return (new Date).getTime()
    });
    var ne = a.toFixed && ("0.000" !== 8e-5.toFixed(3) || "1" !== .9. toFixed(0) || "1.25" !== 1.255.toFixed(2) || "1000000000000000128" !== 0xde0b6b3a7640080.toFixed(0)),
        oe = {
            base: 1e7,
            size: 6,
            data: [0, 0, 0, 0, 0, 0],
            multiply: function(t, e) {
                for (var r = -1; ++r < oe.size;) e += t * oe.data[r], oe.data[r] = e % oe.base, e = Math.floor(e / oe.base)
            },
            divide: function(t) {
                for (var e = oe.size, r = 0; --e >= 0;) r += oe.data[e], oe.data[e] = Math.floor(r / t), r = r % t * oe.base
            },
            numToString: function() {
                for (var t = oe.size, e = ""; --t >= 0;)
                    if ("" !== e || 0 === t || 0 !== oe.data[t]) {
                        var r = String(oe.data[t]);
                        "" === e ? e = r : e += "0000000".slice(0, 7 - r.length) + r
                    }
                return e
            },
            pow: function de(t, e, r) {
                return 0 === e ? r : e % 2 === 1 ? de(t, e - 1, r * t) : de(t * t, e / 2, r)
            },
            log: function(t) {
                for (var e = 0; t >= 4096;) e += 12, t /= 4096;
                for (; t >= 2;) e += 1, t /= 2;
                return e
            }
        };
    E(a, {
        toFixed: function(t) {
            var e, r, n, o, i, a, l, u;
            if (e = Number(t), e = e !== e ? 0 : Math.floor(e), 0 > e || e > 20) throw new RangeError("Number.toFixed called with invalid number of decimals");
            if (r = Number(this), r !== r) return "NaN";
            if (-1e21 >= r || r >= 1e21) return String(r);
            if (n = "", 0 > r && (n = "-", r = -r), o = "0", r > 1e-21)
                if (i = oe.log(r * oe.pow(2, 69, 1)) - 69, a = 0 > i ? r * oe.pow(2, -i, 1) : r / oe.pow(2, i, 1), a *= 4503599627370496, i = 52 - i, i > 0) {
                    for (oe.multiply(0, a), l = e; l >= 7;) oe.multiply(1e7, 0), l -= 7;
                    for (oe.multiply(oe.pow(10, l, 1), 0), l = i - 1; l >= 23;) oe.divide(1 << 23), l -= 23;
                    oe.divide(1 << l), oe.multiply(1, 1), oe.divide(2), o = oe.numToString()
                } else oe.multiply(0, a), oe.multiply(1 << -i, 0), o = oe.numToString() + "0.00000000000000000000".slice(2, 2 + e);
            return e > 0 ? (u = o.length, o = e >= u ? n + "0.0000000000000000000".slice(0, e - u + 2) + o : n + o.slice(0, u - e) + "." + o.slice(u - e)) : o = n + o, o
        }
    }, ne);
    var ie = i.split;
    2 !== "ab".split(/(?:ab)*/).length || 4 !== ".".split(/(.?)(.?)/).length || "t" === "tesst".split(/(s)*/)[1] || 4 !== "test".split(/(?:)/, -1).length || "".split(/.?/).length || ".".split(/()()/).length > 1 ? ! function() {
        var t = "undefined" == typeof /()??/.exec("")[1];
        i.split = function(e, r) {
            var n = this;
            if ("undefined" == typeof e && 0 === r) return [];
            if (!m(e)) return ie.call(this, e, r);
            var o, i, a, l, u = [],
                s = (e.ignoreCase ? "i" : "") + (e.multiline ? "m" : "") + (e.extended ? "x" : "") + (e.sticky ? "y" : ""),
                f = 0;
            for (e = new RegExp(e.source, s + "g"), n += "", t || (o = new RegExp("^" + e.source + "$(?!\\s)", s)), r = "undefined" == typeof r ? -1 >>> 0 : A.ToUint32(r), i = e.exec(n); i && (a = i.index + i[0].length, !(a > f && (u.push(n.slice(f, i.index)), !t && i.length > 1 && i[0].replace(o, function() {
                    for (var t = 1; t < arguments.length - 2; t++) "undefined" == typeof arguments[t] && (i[t] = void 0)
                }), i.length > 1 && i.index < n.length && c.apply(u, i.slice(1)), l = i[0].length, f = a, u.length >= r)));) e.lastIndex === i.index && e.lastIndex++, i = e.exec(n);
            return f === n.length ? (l || !e.test("")) && u.push("") : u.push(n.slice(f)), u.length > r ? u.slice(0, r) : u
        }
    }() : "0".split(void 0, 0).length && (i.split = function(t, e) {
        return "undefined" == typeof t && 0 === e ? [] : ie.call(this, t, e)
    });
    var ae = i.replace,
        le = function() {
            var t = [];
            return "x".replace(/x(.)?/g, function(e, r) {
                t.push(r)
            }), 1 === t.length && "undefined" == typeof t[0]
        }();
    le || (i.replace = function(t, r) {
        var n = e(r),
            o = m(t) && /\)[*?]/.test(t.source);
        if (n && o) {
            var i = function(e) {
                var n = arguments.length,
                    o = t.lastIndex;
                t.lastIndex = 0;
                var i = t.exec(e) || [];
                return t.lastIndex = o, i.push(arguments[n - 2], arguments[n - 1]), r.apply(this, i)
            };
            return ae.call(this, t, i)
        }
        return ae.call(this, t, r)
    });
    var ue = i.substr,
        ce = "".substr && "b" !== "0b".substr(-1);
    E(i, {
        substr: function(t, e) {
            return ue.call(this, 0 > t && (t = this.length + t) < 0 ? 0 : t, e)
        }
    }, ce);
    var se = "	\n\f\r   ᠎             　\u2028\u2029﻿",
        fe = "​",
        pe = "[" + se + "]",
        he = new RegExp("^" + pe + pe + "*"),
        ge = new RegExp(pe + pe + "*$"),
        ye = i.trim && (se.trim() || !fe.trim());
    E(i, {
        trim: function() {
            if ("undefined" == typeof this || null === this) throw new TypeError("can't convert " + this + " to object");
            return String(this).replace(he, "").replace(ge, "")
        }
    }, ye), (8 !== parseInt(se + "08") || 22 !== parseInt(se + "0x16")) && (parseInt = function(t) {
        var e = /^0[xX]/;
        return function(r, n) {
            return r = String(r).trim(), Number(n) || (n = e.test(r) ? 16 : 10), t(r, n)
        }
    }(parseInt))
}),
function() {
    function t(e, n) {
        function i(t) {
            if (i[t] !== d) return i[t];
            var e;
            if ("bug-string-char-index" == t) e = "a" != "a" [0];
            else if ("json" == t) e = i("json-stringify") && i("json-parse");
            else {
                var r, o = '{"a":[1,true,false,null,"\\u0000\\b\\n\\f\\r\\t"]}';
                if ("json-stringify" == t) {
                    var u = n.stringify,
                        s = "function" == typeof u && m;
                    if (s) {
                        (r = function() {
                            return 1
                        }).toJSON = r;
                        try {
                            s = "0" === u(0) && "0" === u(new a) && '""' == u(new l) && u(b) === d && u(d) === d && u() === d && "1" === u(r) && "[1]" == u([r]) && "[null]" == u([d]) && "null" == u(null) && "[null,null,null]" == u([d, b, null]) && u({
                                a: [r, !0, !1, null, "\x00\b\n\f\r	"]
                            }) == o && "1" === u(null, r) && "[\n 1,\n 2\n]" == u([1, 2], null, 1) && '"-271821-04-20T00:00:00.000Z"' == u(new c(-864e13)) && '"+275760-09-13T00:00:00.000Z"' == u(new c(864e13)) && '"-000001-01-01T00:00:00.000Z"' == u(new c(-621987552e5)) && '"1969-12-31T23:59:59.999Z"' == u(new c(-1))
                        } catch (f) {
                            s = !1
                        }
                    }
                    e = s
                }
                if ("json-parse" == t) {
                    var p = n.parse;
                    if ("function" == typeof p) try {
                        if (0 === p("0") && !p(!1)) {
                            r = p(o);
                            var h = 5 == r.a.length && 1 === r.a[0];
                            if (h) {
                                try {
                                    h = !p('"	"')
                                } catch (f) {}
                                if (h) try {
                                    h = 1 !== p("01")
                                } catch (f) {}
                                if (h) try {
                                    h = 1 !== p("1.")
                                } catch (f) {}
                            }
                        }
                    } catch (f) {
                        h = !1
                    }
                    e = h
                }
            }
            return i[t] = !!e
        }
        e || (e = o.Object()), n || (n = o.Object());
        var a = e.Number || o.Number,
            l = e.String || o.String,
            u = e.Object || o.Object,
            c = e.Date || o.Date,
            s = e.SyntaxError || o.SyntaxError,
            f = e.TypeError || o.TypeError,
            p = e.Math || o.Math,
            h = e.JSON || o.JSON;
        "object" == typeof h && h && (n.stringify = h.stringify, n.parse = h.parse);
        var g, y, d, v = u.prototype,
            b = v.toString,
            m = new c(-0xc782b5b800cec);
        try {
            m = -109252 == m.getUTCFullYear() && 0 === m.getUTCMonth() && 1 === m.getUTCDate() && 10 == m.getUTCHours() && 37 == m.getUTCMinutes() && 6 == m.getUTCSeconds() && 708 == m.getUTCMilliseconds()
        } catch (w) {}
        if (!i("json")) {
            var T = "[object Function]",
                O = "[object Date]",
                j = "[object Number]",
                S = "[object String]",
                x = "[object Array]",
                C = "[object Boolean]",
                N = i("bug-string-char-index");
            if (!m) var E = p.floor,
                A = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334],
                I = function(t, e) {
                    return A[e] + 365 * (t - 1970) + E((t - 1969 + (e = +(e > 1))) / 4) - E((t - 1901 + e) / 100) + E((t - 1601 + e) / 400)
                };
            if ((g = v.hasOwnProperty) || (g = function(t) {
                    var e, r = {};
                    return (r.__proto__ = null, r.__proto__ = {
                        toString: 1
                    }, r).toString != b ? g = function(t) {
                        var e = this.__proto__,
                            r = t in (this.__proto__ = null, this);
                        return this.__proto__ = e, r
                    } : (e = r.constructor, g = function(t) {
                        var r = (this.constructor || e).prototype;
                        return t in this && !(t in r && this[t] === r[t])
                    }), r = null, g.call(this, t)
                }), y = function(t, e) {
                    var n, o, i, a = 0;
                    (n = function() {
                        this.valueOf = 0
                    }).prototype.valueOf = 0, o = new n;
                    for (i in o) g.call(o, i) && a++;
                    return n = o = null, a ? y = 2 == a ? function(t, e) {
                        var r, n = {},
                            o = b.call(t) == T;
                        for (r in t) o && "prototype" == r || g.call(n, r) || !(n[r] = 1) || !g.call(t, r) || e(r)
                    } : function(t, e) {
                        var r, n, o = b.call(t) == T;
                        for (r in t) o && "prototype" == r || !g.call(t, r) || (n = "constructor" === r) || e(r);
                        (n || g.call(t, r = "constructor")) && e(r)
                    } : (o = ["valueOf", "toString", "toLocaleString", "propertyIsEnumerable", "isPrototypeOf", "hasOwnProperty", "constructor"], y = function(t, e) {
                        var n, i, a = b.call(t) == T,
                            l = !a && "function" != typeof t.constructor && r[typeof t.hasOwnProperty] && t.hasOwnProperty || g;
                        for (n in t) a && "prototype" == n || !l.call(t, n) || e(n);
                        for (i = o.length; n = o[--i]; l.call(t, n) && e(n));
                    }), y(t, e)
                }, !i("json-stringify")) {
                var M = {
                        92: "\\\\",
                        34: '\\"',
                        8: "\\b",
                        12: "\\f",
                        10: "\\n",
                        13: "\\r",
                        9: "\\t"
                    },
                    D = "000000",
                    U = function(t, e) {
                        return (D + (e || 0)).slice(-t)
                    },
                    _ = "\\u00",
                    F = function(t) {
                        for (var e = '"', r = 0, n = t.length, o = !N || n > 10, i = o && (N ? t.split("") : t); n > r; r++) {
                            var a = t.charCodeAt(r);
                            switch (a) {
                                case 8:
                                case 9:
                                case 10:
                                case 12:
                                case 13:
                                case 34:
                                case 92:
                                    e += M[a];
                                    break;
                                default:
                                    if (32 > a) {
                                        e += _ + U(2, a.toString(16));
                                        break
                                    }
                                    e += o ? i[r] : t.charAt(r)
                            }
                        }
                        return e + '"'
                    },
                    J = function(t, e, r, n, o, i, a) {
                        var l, u, c, s, p, h, v, m, w, T, N, A, M, D, _, k;
                        try {
                            l = e[t]
                        } catch (P) {}
                        if ("object" == typeof l && l)
                            if (u = b.call(l), u != O || g.call(l, "toJSON")) "function" == typeof l.toJSON && (u != j && u != S && u != x || g.call(l, "toJSON")) && (l = l.toJSON(t));
                            else if (l > -1 / 0 && 1 / 0 > l) {
                            if (I) {
                                for (p = E(l / 864e5), c = E(p / 365.2425) + 1970 - 1; I(c + 1, 0) <= p; c++);
                                for (s = E((p - I(c, 0)) / 30.42); I(c, s + 1) <= p; s++);
                                p = 1 + p - I(c, s), h = (l % 864e5 + 864e5) % 864e5, v = E(h / 36e5) % 24, m = E(h / 6e4) % 60, w = E(h / 1e3) % 60, T = h % 1e3
                            } else c = l.getUTCFullYear(), s = l.getUTCMonth(), p = l.getUTCDate(), v = l.getUTCHours(), m = l.getUTCMinutes(), w = l.getUTCSeconds(), T = l.getUTCMilliseconds();
                            l = (0 >= c || c >= 1e4 ? (0 > c ? "-" : "+") + U(6, 0 > c ? -c : c) : U(4, c)) + "-" + U(2, s + 1) + "-" + U(2, p) + "T" + U(2, v) + ":" + U(2, m) + ":" + U(2, w) + "." + U(3, T) + "Z"
                        } else l = null;
                        if (r && (l = r.call(e, t, l)), null === l) return "null";
                        if (u = b.call(l), u == C) return "" + l;
                        if (u == j) return l > -1 / 0 && 1 / 0 > l ? "" + l : "null";
                        if (u == S) return F("" + l);
                        if ("object" == typeof l) {
                            for (D = a.length; D--;)
                                if (a[D] === l) throw f();
                            if (a.push(l), N = [], _ = i, i += o, u == x) {
                                for (M = 0, D = l.length; D > M; M++) A = J(M, l, r, n, o, i, a), N.push(A === d ? "null" : A);
                                k = N.length ? o ? "[\n" + i + N.join(",\n" + i) + "\n" + _ + "]" : "[" + N.join(",") + "]" : "[]"
                            } else y(n || l, function(t) {
                                var e = J(t, l, r, n, o, i, a);
                                e !== d && N.push(F(t) + ":" + (o ? " " : "") + e)
                            }), k = N.length ? o ? "{\n" + i + N.join(",\n" + i) + "\n" + _ + "}" : "{" + N.join(",") + "}" : "{}";
                            return a.pop(), k
                        }
                    };
                n.stringify = function(t, e, n) {
                    var o, i, a, l;
                    if (r[typeof e] && e)
                        if ((l = b.call(e)) == T) i = e;
                        else if (l == x) {
                        a = {};
                        for (var u, c = 0, s = e.length; s > c; u = e[c++], l = b.call(u), (l == S || l == j) && (a[u] = 1));
                    }
                    if (n)
                        if ((l = b.call(n)) == j) {
                            if ((n -= n % 1) > 0)
                                for (o = "", n > 10 && (n = 10); o.length < n; o += " ");
                        } else l == S && (o = n.length <= 10 ? n : n.slice(0, 10));
                    return J("", (u = {}, u[""] = t, u), i, a, o, "", [])
                }
            }
            if (!i("json-parse")) {
                var k, P, R = l.fromCharCode,
                    Z = {
                        92: "\\",
                        34: '"',
                        47: "/",
                        98: "\b",
                        116: "	",
                        110: "\n",
                        102: "\f",
                        114: "\r"
                    },
                    $ = function() {
                        throw k = P = null, s()
                    },
                    z = function() {
                        for (var t, e, r, n, o, i = P, a = i.length; a > k;) switch (o = i.charCodeAt(k)) {
                            case 9:
                            case 10:
                            case 13:
                            case 32:
                                k++;
                                break;
                            case 123:
                            case 125:
                            case 91:
                            case 93:
                            case 58:
                            case 44:
                                return t = N ? i.charAt(k) : i[k], k++, t;
                            case 34:
                                for (t = "@", k++; a > k;)
                                    if (o = i.charCodeAt(k), 32 > o) $();
                                    else if (92 == o) switch (o = i.charCodeAt(++k)) {
                                    case 92:
                                    case 34:
                                    case 47:
                                    case 98:
                                    case 116:
                                    case 110:
                                    case 102:
                                    case 114:
                                        t += Z[o], k++;
                                        break;
                                    case 117:
                                        for (e = ++k, r = k + 4; r > k; k++) o = i.charCodeAt(k), o >= 48 && 57 >= o || o >= 97 && 102 >= o || o >= 65 && 70 >= o || $();
                                        t += R("0x" + i.slice(e, k));
                                        break;
                                    default:
                                        $()
                                } else {
                                    if (34 == o) break;
                                    for (o = i.charCodeAt(k), e = k; o >= 32 && 92 != o && 34 != o;) o = i.charCodeAt(++k);
                                    t += i.slice(e, k)
                                }
                                if (34 == i.charCodeAt(k)) return k++, t;
                                $();
                            default:
                                if (e = k, 45 == o && (n = !0, o = i.charCodeAt(++k)), o >= 48 && 57 >= o) {
                                    for (48 == o && (o = i.charCodeAt(k + 1), o >= 48 && 57 >= o) && $(), n = !1; a > k && (o = i.charCodeAt(k), o >= 48 && 57 >= o); k++);
                                    if (46 == i.charCodeAt(k)) {
                                        for (r = ++k; a > r && (o = i.charCodeAt(r), o >= 48 && 57 >= o); r++);
                                        r == k && $(), k = r
                                    }
                                    if (o = i.charCodeAt(k), 101 == o || 69 == o) {
                                        for (o = i.charCodeAt(++k), (43 == o || 45 == o) && k++, r = k; a > r && (o = i.charCodeAt(r), o >= 48 && 57 >= o); r++);
                                        r == k && $(), k = r
                                    }
                                    return +i.slice(e, k)
                                }
                                if (n && $(), "true" == i.slice(k, k + 4)) return k += 4, !0;
                                if ("false" == i.slice(k, k + 5)) return k += 5, !1;
                                if ("null" == i.slice(k, k + 4)) return k += 4, null;
                                $()
                        }
                        return "$"
                    },
                    H = function(t) {
                        var e, r;
                        if ("$" == t && $(), "string" == typeof t) {
                            if ("@" == (N ? t.charAt(0) : t[0])) return t.slice(1);
                            if ("[" == t) {
                                for (e = []; t = z(), "]" != t; r || (r = !0)) r && ("," == t ? (t = z(), "]" == t && $()) : $()), "," == t && $(), e.push(H(t));
                                return e
                            }
                            if ("{" == t) {
                                for (e = {}; t = z(), "}" != t; r || (r = !0)) r && ("," == t ? (t = z(), "}" == t && $()) : $()), ("," == t || "string" != typeof t || "@" != (N ? t.charAt(0) : t[0]) || ":" != z()) && $(), e[t.slice(1)] = H(z());
                                return e
                            }
                            $()
                        }
                        return t
                    },
                    Y = function(t, e, r) {
                        var n = B(t, e, r);
                        n === d ? delete t[e] : t[e] = n
                    },
                    B = function(t, e, r) {
                        var n, o = t[e];
                        if ("object" == typeof o && o)
                            if (b.call(o) == x)
                                for (n = o.length; n--;) Y(o, n, r);
                            else y(o, function(t) {
                                Y(o, t, r)
                            });
                        return r.call(t, e, o)
                    };
                n.parse = function(t, e) {
                    var r, n;
                    return k = 0, P = "" + t, r = H(z()), "$" != z() && $(), k = P = null, e && b.call(e) == T ? B((n = {}, n[""] = r, n), "", e) : r
                }
            }
        }
        return n.runInContext = t, n
    }
    var e = "function" == typeof define && define.amd,
        r = {
            "function": !0,
            object: !0
        },
        n = r[typeof exports] && exports && !exports.nodeType && exports,
        o = r[typeof window] && window || this,
        i = n && r[typeof module] && module && !module.nodeType && "object" == typeof global && global;
    if (!i || i.global !== i && i.window !== i && i.self !== i || (o = i), n && !e) t(o, n);
    else {
        var a = o.JSON,
            l = o.JSON3,
            u = !1,
            c = t(o, o.JSON3 = {
                noConflict: function() {
                    return u || (u = !0, o.JSON = a, o.JSON3 = l, a = l = null), c
                }
            });
        o.JSON = {
            parse: c.parse,
            stringify: c.stringify
        }
    }
    e && define(function() {
        return c
    })
}.call(this);
