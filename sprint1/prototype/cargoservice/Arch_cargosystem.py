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
          cargoservice=Custom('cargoservice','./qakicons/symActorWithobjSmall.png')
     with Cluster('ctxcustomer', graph_attr=nodeattr):
          ledmock=Custom('ledmock(ext)','./qakicons/externalQActor.png')
          ioportmock=Custom('ioportmock(ext)','./qakicons/externalQActor.png')
     with Cluster('ctxdevices', graph_attr=nodeattr):
          hold=Custom('hold(ext)','./qakicons/externalQActor.png')
          sonarmock=Custom('sonarmock(ext)','./qakicons/externalQActor.png')
          markerdevice=Custom('markerdevice(ext)','./qakicons/externalQActor.png')
     with Cluster('ctxrobot', graph_attr=nodeattr):
          cargorobotmock=Custom('cargorobotmock(ext)','./qakicons/externalQActor.png')
     sys >> Edge( label='sonardata', **evattr, decorate='true', fontcolor='darkgreen') >> cargoservice
     cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<robot_move<font color="darkgreen"> robot_done</font> &nbsp; >',  fontcolor='magenta') >> cargorobotmock
     cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<mark_container<font color="darkgreen"> marking_done</font> &nbsp; >',  fontcolor='magenta') >> markerdevice
     cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<get_slot<font color="darkgreen"> slot_reserved hold_full</font> &nbsp; >',  fontcolor='magenta') >> hold
     cargoservice >> Edge(color='blue', style='solid',  decorate='true', label='<led_ctrl &nbsp; >',  fontcolor='blue') >> ledmock
     cargoservice >> Edge(color='blue', style='solid',  decorate='true', label='<free_slot &nbsp; >',  fontcolor='blue') >> hold
diag
