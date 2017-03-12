<%-- 
    Document   : Catalog
    Created on : Feb 19, 2014, 10:06:49 PM
    Author     : Gezimi
--%>
<!DOCTYPE html>
<html>

    <jsp:include page="Header.jsp" />    
    <header class="masthead">
        <div class="jumbotron">
            <div class="container">
                <div class="page-header_new">
                    <h1>Data Catalog</h1>
                </div>
              <div id="search-helper-message">
                    Search over catalog of Wiki Web-Sites.
                </div>
            </div>
            </div>
    </header>

        <div class="container">
            <div class="toolbar">               
                <ol class="breadcrumb">
                    <li class="home"><a href="index.jsp"><i class="icon-home"></i><span> Home</span></a></li>
                    <li class="active">Catalog</li>
                </ol>
            </div>
            <div class="primary">
                <section class="module">
                    <div class="module-content">
                        <form id="dataset-search" class="search-form form-inline no-padding col-md-12 col-lg-12" method="get" data-module="select-switch">
                            <div class="input-group">
                                <input type="search" id="search-header" title="Example searches: Baseball, FIEK" data-strings='{"targets":["Category",""]}' value="<%
                                    if (request.getParameter("Search") != null) {
                                        out.print(request.getParameter("Search"));
                                    } else {
                                        out.print("");
                                    }
                                       %>" name="Search" class="search-field form-control" autocomplete="off" placeholder="Search catalog..." >
                                <span class="input-group-btn">
                                    <button type="submit" class="search-submit btn btn-primary">
                                        <i class="fa fa-search"></i>
                                        <span class="sr-only">Search</span>
                                    </button>
                                </span>
                            </div>
                        </form>
                        <div class="results">
                            <jsp:include page="Jena_Query.jsp" />                           
                        </div>
                    </div><!--/.container-->
                </section>
            </div>
        </div>

<jsp:include page="Footer.jsp"/>
</html>
