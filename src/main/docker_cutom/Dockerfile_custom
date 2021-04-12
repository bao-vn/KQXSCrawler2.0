FROM gcr.io/google-appengine/jetty
ADD kqxs-crawler-1.0.war $JETTY_BASE/webapps/root.war
WORKDIR $JETTY_BASE
RUN java -jar $JETTY_HOME/start.jar --approve-all-licenses --add-to-startd=jmx,stats,hawtio && chown -R jetty:jetty $JETTY_BASE