### conda install diagrams
from diagrams import Cluster, Diagram, Edge
from diagrams.custom import Custom
import os
os.environ['PATH'] += os.pathsep + 'C:/Program Files/Graphviz/bin/'

graphattr = {     #https://www.graphviz.org/doc/info/attrs.html
    'fontsize': '22',
}

nodeattr = {   
    'fontsize': '22',
    'bgcolor': 'lightyellow'
}

eventedgeattr = {
    'color': 'red',
    'style': 'dotted'
}
evattr = {
    'color': 'darkgreen',
    'style': 'dotted'
}
with Diagram('cargosystemArch', show=False, outformat='png', graph_attr=graphattr) as diag:
  with Cluster('env'):
     sys = Custom('','./qakicons/system.png')
### see https://renenyffenegger.ch/notes/tools/Graphviz/attributes/label/HTML-like/index
     with Cluster('ctxcargoservice', graph_attr=nodeattr):
          cargoservice=Custom('cargoservice(ext)','./qakicons/externalQActor.png')
     with Cluster('ctxdevices', graph_attr=nodeattr):
          sonaradapter=Custom('sonaradapter','./qakicons/symActorWithobjSmall.png')
          markerdevice=Custom('markerdevice','./qakicons/symActorWithobjSmall.png')
     sys >> Edge( label='kernel_rawmsg', **evattr, decorate='true', fontcolor='darkgreen') >> sonaradapter
     sonaradapter >> Edge( label='sonardata', **eventedgeattr, decorate='true', fontcolor='red') >> sys
diag
