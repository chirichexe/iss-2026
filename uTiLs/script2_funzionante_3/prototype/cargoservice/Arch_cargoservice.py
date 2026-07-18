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
with Diagram('cargoserviceArch', show=False, outformat='png', graph_attr=graphattr) as diag:
  with Cluster('env'):
     sys = Custom('','./qakicons/system.png')
### see https://renenyffenegger.ch/notes/tools/Graphviz/attributes/label/HTML-like/index
     with Cluster('ctxcargoservice', graph_attr=nodeattr):
          cargoservice=Custom('cargoservice','./qakicons/symActorWithobjSmall.png')
     with Cluster('ctxioport', graph_attr=nodeattr):
          ledadapter=Custom('ledadapter(ext)','./qakicons/externalQActor.png')
     with Cluster('ctxdevices', graph_attr=nodeattr):
          markerdevice=Custom('markerdevice(ext)','./qakicons/externalQActor.png')
     with Cluster('ctxrobot', graph_attr=nodeattr):
          cargorobot=Custom('cargorobot(ext)','./qakicons/externalQActor.png')
     cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<mark_container<font color="darkgreen"> marking_done</font> &nbsp; >',  fontcolor='magenta') >> markerdevice
     cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<moverobot<font color="darkgreen"> moverobotdone moverobotfailed</font> &nbsp; >',  fontcolor='magenta') >> cargorobot
     cargoservice >> Edge(color='blue', style='solid',  decorate='true', label='<led_ctrl &nbsp; >',  fontcolor='blue') >> ledadapter
diag
