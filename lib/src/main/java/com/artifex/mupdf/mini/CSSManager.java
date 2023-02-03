package com.artifex.mupdf.mini;

import java.util.HashMap;
import java.util.Map;

public class CSSManager {

    public int fontSize = 20; // 1-80
    public String textAlign = "justify"; // justify/left

    private final static String BASE_CSS =
            "a{color:#06C;text-decoration:underline}\n" +
            "address{display:block;font-style:italic}\n" +
            "b{font-weight:bold}\n" +
            "bdo{direction:rtl;unicode-bidi:bidi-override}\n" +
            "blockquote{display:block;margin:1em 40px}\n" +
            "\n" +
            "cite{font-style:italic}\n" +
            "code{font-family:monospace}\n" +
            "dd{display:block;margin:0 0 0 40px}\n" +
            "del{text-decoration:line-through}\n" +
            "div{display:block}\n" +
            "dl{display:block;margin:1em 0}\n" +
            "dt{display:block}\n" +
            "em{font-style:italic}\n" +
            "\n" +
            "h1{display:block;font-size:1.5em;  margin:0.67em 0;page-break-after:avoid}\n" +
            "h2{display:block;font-size:1.3em;  margin:0.83em 0;page-break-after:avoid}\n" +
            "h3{display:block;font-size:1.1em;  margin:1em 0;page-break-after:avoid}\n" +
            "h4{display:block;font-size:1em;    margin:1.33em 0;page-break-after:avoid}\n" +
            "h5{display:block;font-size:0.8em; margin:1.67em 0;page-break-after:avoid}\n" +
            "h6{display:block;font-size:0.6em; margin:2.33em 0;page-break-after:avoid}\n" +
            "\n" +
            "head{display:none}\n" +
            "hr{border-style:solid;border-width:1px;display:block;margin-bottom:0.5em;margin-top:0.5em;text-align:center}\n" +
            "html{display:block}\n" +
            "i{font-style:italic}\n" +
            "ins{text-decoration:underline}\n" +
            "kbd{font-family:monospace}\n" +
            "li{display:list-item}\n" +
            "menu{display:block;list-style-type:disc;margin:1em 0;padding:0 1em 0 1em}\n" +
            "ol{display:block;list-style-type:decimal;margin:1em;padding:0 1em 0 1em}\n" +
            "p{display:block;}\n" +
            "pre{display:block;font-family:monospace;margin:1em 0;white-space:pre}\n" +
            "samp{font-family:monospace}\n" +
            "script{display:none}\n" +
            "small{font-size:0.83em}\n" +
            "x-small{font-size:0.6em}\n" +
            "strong{font-weight:bold}\n" +
            "style{display:none}\n" +
            "sub{font-size:0.83em;vertical-align:sub}\n" +
            "\n" +
            "table{display:block !important; margin:1em !important;font-size:0.8em;}\n" +
            "tr,thead,tfoot   {display:block !important;margin-top:1em !important;}\n" +
            "td,th {display:block !important;border-style:solid; border-width:1px; padding:0.1em 0 0.1em 0.5em}\n" +
            "tbody {display:block !important;}\n" +
            "th{font-weight:bold; text-align:center}\n" +
            "\n" +
            "ul{display:block;list-style-type:disc;margin:1em;padding:0 0 0 30pt}\n" +
            "ul ul{list-style-type:circle}\n" +
            "ul ul ul{list-style-type:square}\n" +
            "var{font-style:italic}\n" +
            "figcaption {display:block; text-align:center}\n" +
            "figcaption>p {text-align:center}\n" +
            "br{display:block}\n" +
            "\n" +
            "FictionBook{display:block}\n" +
            "stylesheet,binary{display:none}\n" +
            "description,description>*{display:none}\n" +
            "description>title-info{display:block}\n" +
            "description>title-info>*{display:none}\n" +
            "description>title-info>annotation{display:block;page-break-before:always;page-break-after:always}\n" +
            "description>title-info>coverpage{display:block;page-break-before:always;page-break-after:always}\n" +
            "body,section,title,subtitle,p,cite,epigraph,text-author,date,poem,stanza,v,empty-line{display:block}\n" +
            "image{display:block}\n" +
            "\n" +
            "table{display:block !important; margin:1em !important;font-size:0.8em;}\n" +
            "tr,thead,tfoot   {display:block !important;margin-top:1em !important;}\n" +
            "td,th {display:block !important;border-style:solid; border-width:1px; padding:0.1em 0 0.1em 0.5em}\n" +
            "tbody {display:block !important;}\n" +
            "th{font-weight:bold; text-align:center}\n" +
            "\n" +
            "a{color:#06C;text-decoration:underline}\n" +
            "a[type=note]{font-size:small;vertical-align:super}\n" +
            "code{white-space:pre;font-family:monospace}\n" +
            "emphasis{font-style:italic}\n" +
            "strikethrough{text-decoration:line-through}\n" +
            "strong{font-weight:bold}\n" +
            "sub{font-size:small;vertical-align:sub}\n" +
            "image{margin:1em 0;text-align:center}\n" +
            "cite,poem{margin:1em 1.5em}\n" +
            "subtitle,epigraph,stanza{margin:1em 0}\n" +
            "empty-line{padding-top:1em}\n" +
            "\n" +
            "section>title{page-break-before:avoid;}\n" +
            "section>title>p{text-align:center !important; text-indent:0px !important;}\n" +
            "title>p{text-align:center !important; text-indent:0px !important;}\n" +
            "subtitle{text-align:center !important; text-indent:0px !important;}\n" +
            "image{text-align:center; text-indent:0px;}\n" +
            "section+section>title{page-break-before:always;}\n" +
            "epigraph{text-align:right; margin-left:2em;font-style: italic;}\n" +
            "text-author{font-style: italic;font-weight: bold;}\n" +
            "del,ins,u,strikethrough{font-family:monospace;}\n" +
            "\n" +
            "text-author{font-style: italic;font-weight: bold;}\n" +
            "p>image{display:block;}\n" +
            "\n" +
            "b>span,strong>span{font-weight:normal}\n" +
            "book>title, bookinfo {display:none}\n" +
            "\n" +
            "svg{display:block}\n" +
            "math, m, svg>text {display:none}\n" +
            "sup,sup>* {font-size:0.83em;vertical-align:super; font-weight:bold}\n" +
            "\n" +
            "body{display:block; padding:0 !important; margin:0 !important;}\n" +
            "\n" +
            "h1,h2,h3,h4,h5,h6,img {text-indent:0px !important; text-align: center;}\n" +
            "\n" +
            "title,title>p,title>p>strong{font-size:1.2em;}\n" +
            "subtitle{font-size:1.0em;}\n" +
            "\n" +
            "t{font-style: italic;}";

    public CSSManager() {}

    public String getCSS() {
        return BASE_CSS +
                "body{font-size: "+getFontSizePercent(fontSize)+"% !important;}\n"+
                "p{text-align:"+textAlign+";}\n";
    }

    private static int getFontSizePercent(int value) {
        return 500+25*value;
    }


}
