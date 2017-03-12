<%-- 
    Document   : index
    Created on : Feb 16, 2014, 1:56:24 PM
    Author     : Gezimi
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<jsp:include page="Header.jsp" flush="true" />
<div class="jumbotron">
  <div class="container">
    <h1>The home of the Semantic Search Wiki </h1>
<p>Search over Wikipedia dump pages, their links, calculated rank and original rank from Wikipedia, and more!</p>

<div style="display: block !important; margin:0 !important; padding: 0 !important" id="wpp_popup_post_end_element"></div>  </div><!--/.container-->
</div><!--/.jumbotron-->
<div class="header banner">
    <div class="container">
      <div class="text-center getstarted"><h4><label for="search-header"><strong>START SEARCHING</strong><br><small>Over thousands wiki pages</small><br /><i class="fa fa-caret-down"></i></label></h4></div>
      <form role="search" method="get" class="search-form form-inline no-padding col-md-12 col-lg-12" action="Catalog.jsp">
  <div class="input-group">            
    <input type="search" id="search-header" title="Example searches: Baseball, FIEK" data-strings='{"targets":["Category",""]}' value="" name="Search" class="search-field form-control" placeholder="Search Wiki Web-Site&#8217;s">
      <span class="input-group-btn">
      <button type="submit" class="search-submit btn btn-primary">
           <i class="fa fa-search"></i>
           <span class="sr-only">Search</span>
       </button>
    </span>
  </div>
</form>  
     </div><!--/.container-->   
</div>

<div role="document">
    <div class="content">
      <main class="main" role="main" id="main">
        <div class="wrap container">
<div class="page-header">
  <h1>SEMANTIC WEB</h1>  
<blockquote>
In addition to the classic “Web of documents” W3C is helping to build a technology stack to support a “Web of data,” the sort of data you find in databases. The ultimate goal of the Web of data is to enable computers to do more useful work and to develop systems that can support trusted interactions over the network. The term “Semantic Web” refers to W3C’s vision of the Web of linked data. 
Semantic Web technologies enable people to create data stores on the Web, build vocabularies, and write rules for handling data. Linked data are empowered by technologies such as RDF, SPARQL, OWL, and SKOS.
</blockquote>    

<div class="container">
      <!-- Example row of columns -->
      <div class="row">
        <div class="col-md-4">
            <h2>Linked Data</h2>
          <p>The Semantic Web is a Web of data — of dates and titles and part numbers and chemical properties and any other data one might conceive of. RDF provides the foundation for publishing and linking your data. Various technologies allow you to embed data in documents (RDFa, GRDDL) or expose what you have in SQL databases, or make it available as RDF files. </p>
          <p><a class="btn btn-primary btn-large" href="http://www.w3.org/standards/semanticweb/data" role="button" target="_blank">View more &raquo;</a></p>
        </div>
        <div class="col-md-4">
           <h2>Vocabularies</h2>
          <p>At times it may be important or valuable to organize data. Using OWL (to build vocabularies, or “ontologies”) and SKOS (for designing knowledge organization systems) it is possible to enrich data with additional meaning, which allows more people (and more machines) to do more with the data. </p>
          <p><a class="btn btn-primary btn-large" href="http://www.w3.org/standards/semanticweb/ontology" role="button" target="_blank">View more &raquo;</a></p>
       </div>
        <div class="col-md-4">
         <h2> Query</h2>
          <p>Query languages go hand-in-hand with databases. If the Semantic Web is viewed as a global database, then it is easy to understand why one would need a query language for that data. SPARQL is the query language for the Semantic Web. </p>
          <p><a class="btn btn-primary btn-large" href="http://www.w3.org/standards/semanticweb/query" role="button" target="_blank">View more &raquo;</a></p>
        </div>
      </div>
      <hr>
    </div> <!-- /container -->
    </div>
</div>
    </div>
<jsp:include page="Footer.jsp" flush="true" />
</html>
