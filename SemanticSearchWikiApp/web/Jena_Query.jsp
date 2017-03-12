<%-- 
    Document   : Jena_Query
    Created on : Feb 26, 2014, 1:28:32 PM
    Author     : Gezimi
--%>

<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="com.hp.hpl.jena.ontology.*" %>
<%@ page import="com.hp.hpl.jena.rdf.model.*" %>
<%@ page import="com.hp.hpl.jena.util.*" %>
<%@ page import="com.hp.hpl.jena.query.*" %>
<%@ page import="com.hp.hpl.jena.n3.IRIResolver" %>
<%@ page import="java.net.URI" %>
<%@ page import="edu.stanford.smi.protegex.owl.jena.JenaOWLModel" %>
<%@ page import="edu.stanford.smi.protegex.owl.ProtegeOWL" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    String ONTOLOGY_URL = ".../SemanticSearchOnto-v.1.0.owl";
    String ONTOLOGY_URL_wiki = ".../wikipediaOnto.owl";
    String dataprfx = "prefix  base: <http://localhost/Institute#>";
    String rdfprfx = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
    String rdfsprfx = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
    String portalprfx = "prefix portal: <http://www.aktors.org/ontology/portal#> ";
    String wikiprfx = "prefix wiki: <http://localhost:2020/#> ";

    String QuerySearch = "";
    if (request.getParameter("Search") != null) {
        QuerySearch = request.getParameter("Search");
    }

    String inputQuery = dataprfx + rdfprfx + portalprfx + wikiprfx + ""
            + "SELECT ?WebSite ?OutLinks ?PR ?wikiR ?x "
            + "WHERE { "
            + "?x rdf:type portal:Web-Site."
            + "?x portal:has-title ?WebSite."
            + "FILTER regex(?WebSite, '" + QuerySearch + "', 'i' ) "
            + " optional{?x base:hasTotalNrOutLinks ?OutLinks}."
            + " optional{?x base:hasWebPR ?PR}."
            + " optional{?x wiki:has-page_random ?wikiR}."
            + "} "
            + "ORDER BY DESC(?PR) ";
    try {
        // Get the start time of the process
        long start = System.currentTimeMillis();

        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        OntDocumentManager dm = model.getDocumentManager();

        InputStream in = FileManager.get().open(ONTOLOGY_URL);
        if (in == null) {
            throw new IllegalArgumentException("File: " + ONTOLOGY_URL + " not found");
        }
        InputStream in_wiki = FileManager.get().open(ONTOLOGY_URL_wiki);
        if (in_wiki == null) {
            throw new IllegalArgumentException("File: " + ONTOLOGY_URL_wiki + " not found");
        }
        dm.addAltEntry(ONTOLOGY_URL, ONTOLOGY_URL);
        model.read(in, "");
        model.read(in_wiki,"");

        
        // create a Jena query from the queryString
        Query query = QueryFactory.create(inputQuery);

        // create a Jena QueryExecution object that knows the query 
        // and the model over which the query will be run
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        try {
            // execute the query - get back a ResultSet 
            ResultSet resultSet = qexec.execSelect();
            List resultVars = resultSet.getResultVars();
            int colCount = resultVars.size();
            
            out.print("<div class='container'><table>");
          
            int y = 0;
            // iterate over the result set 
            while (resultSet.hasNext()) {
                out.print("<tr>");
                QuerySolution querySolution = resultSet.nextSolution();
                RDFNode nodeWebSite = querySolution.get((String) resultVars.get(0));
                RDFNode nodeOutLinks = querySolution.get((String) resultVars.get(1));
                RDFNode nodePR = querySolution.get((String) resultVars.get(2));
                RDFNode nodewikiR = querySolution.get((String) resultVars.get(3));
                RDFNode nodeUri = querySolution.get((String) resultVars.get(4));
                out.print("<td class=\"sqltabell\">");
                Literal litWebSite = (Literal) nodeWebSite, litOutLinks = (Literal) nodeOutLinks, litPR = (Literal) nodePR, litwikiR = (Literal) nodewikiR;
                Resource resUri = (Resource) nodeUri;
                String sWebSite = "", sOutLinks = "0", sPR = "0.00", sUri = "", swikiR = "";
                if (litWebSite != null) {
                    sWebSite = litWebSite.getString();
                }
                if (litOutLinks != null) {
                    sOutLinks = litOutLinks.getString();
                }
                if (litPR != null) {
                    sPR = litPR.getString();
                }
                if (litwikiR != null) {
                    swikiR = litwikiR.getString();
                }
                if (resUri != null & nodeUri.isURIResource()) {
                    sUri = resUri.getURI();
                }
                y++;
              
                out.print("<strong><a href='http://wikipedia.org/wiki/" + sWebSite + "' target='_blank'>" + sWebSite + "</a></strong><tr><td>");
                out.print("<small>" + sUri + "</small></td></tr><tr><td>");
                out.print("<blockquote><strong><i class=' icon-chevron-down' title='Semantic PageRank of Web-Site.'></i>&nbsp" + sPR + "&nbsp");
                out.print(" <i class='icon-globe' title='Number of Out Links.'></i>&nbsp" + sOutLinks + "&nbsp");
                out.print(" <i class='icon-random' title='The wikipedia rank number.'></i>&nbsp" + swikiR + "</strong></blockquote></td><tr></tr></tr>");
                out.print("</tr>");
            }
            // Get the end time of the process
            long end = System.currentTimeMillis();
            long elapsedTime = end - start;

            if (!QuerySearch.isEmpty()) {
                out.print("<strong>" + y + " results found for &#34;" + QuerySearch + "&#34;  (</strong><small>About  " + elapsedTime + " ms</small><strong>).</strong>");
            } else {
                out.print("<strong>" + y + " results found (</strong><small>About  " + elapsedTime + " ms</small><strong>).</strong>");
            }
            out.print("</p><hr class='fancy-line'></hr>");

        } finally {
            qexec.close();
        }
    } catch (Exception e) {
        out.print("<tr><td><xmp>" + e + "</xmp></td></tr>");
    }
    out.print("</table></div>");
    out.print("<hr class='fancy-line'></hr>");

%>
