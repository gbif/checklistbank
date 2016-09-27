from fabric.api import env
from fabric.operations import put
from fabric.context_managers import cd

env.hosts = ['root@'+h+'.gbif.org' for h in ["c1n1","c1n2","c1n3","c1n6","c2n1","c2n2","c2n3","bantha","devgateway-vh"]]
SOLR_LIB = '/opt/cloudera/parcels/SOLR5/server/solr-webapp/webapp/WEB-INF/lib'

def remove():
    with cd(SOLR_LIB):
        run('rm -f checklistbank-solr-plugins.jar')

def copy():
    put('../target/checklistbank-solr-plugins.jar', SOLR_LIB)
