<%-- 
    Document   : AboutUs
    Created on : Feb 19, 2014, 1:44:23 PM
    Author     : Gezimi
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <jsp:include page="Header.jsp" flush="true" />
      
    <section id="highlights" class="wrap wrap-lightblue">
        <div class="container">
          <br></br>
            <div class="page-header">  
                <h1>Semantic Search Wiki</h1>         
            <div class="highlight ">
                <header>
                    <h2 class="entry-title">About project</h2>
                </header>
                <article class="no-image">                    
                    <p>
                        This project is part of my Master Thesis “Ranking Semantically Web Pages – Wikipedia as Case Study”, which include searching over web-pages of dump <a href="http://www.wikipedia.org/" class="external ext-link" rel="external" onclick="this.target='_blank';">Wikipedia</a>, mapped and converted to format .owl, using SWRL rules and SPARQL queries.
                    </p>

                    <div style="display: block !important; margin:0 !important; padding: 0 !important" id="wpp_popup_post_end_element"></div>		</article>
            </div><!--/.highlight-->
        </div><!--/.container-->
    </section><!--/.wrap-lightblue-->
  
    <jsp:include page="Footer.jsp" flush="true" />
</html>
