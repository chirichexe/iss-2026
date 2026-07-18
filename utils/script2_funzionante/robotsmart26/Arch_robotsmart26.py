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
with Diagram('robotsmart26Arch', show=False, outformat='png', graph_attr=graphattr) as diag:
  with Cluster('env'):
     sys = Custom('','./qakicons/system.png')
### see https://renenyffenegger.ch/notes/tools/Graphviz/attributes/label/HTML-like/index
     with Cluster('ctxrobotsmart', graph_attr=nodeattr):
          robotsmart=Custom('robotsmart','./qakicons/symActorWithobjSmall.png')
          robotmnemo=Custom('robotmnemo','./qakicons/symActorWithobjSmall.png')
          planexec=Custom('planexec','./qakicons/symActorWithobjSmall.png')
     robotmnemo >> Edge( label='sonardata', **eventedgeattr, decorate='true', fontcolor='red') >> robotmnemo
     robotmnemo >> Edge( label='vrinfo', **eventedgeattr, decorate='true', fontcolor='red') >> robotmnemo
     robotmnemo >> Edge( label='sonaralarm', **eventedgeattr, decorate='true', fontcolor='red') >> sys
     sys >> Edge( label='alarm', **evattr, decorate='true', fontcolor='darkgreen') >> planexec
     robotsmart >> Edge(color='magenta', style='solid', decorate='true', label='<doplan<font color="darkgreen"> doplandone doplanfailed</font> &nbsp; >',  fontcolor='magenta') >> planexec
     planexec >> Edge(color='magenta', style='solid', decorate='true', label='<step<font color="darkgreen"> stepdone stepfailed</font> &nbsp; >',  fontcolor='magenta') >> robotmnemo
     robotmnemo >> Edge(color='magenta', style='solid', decorate='true', label='<doplan<font color="darkgreen"> doplandone doplanfailed</font> &nbsp; >',  fontcolor='magenta') >> planexec
     robotsmart >> Edge(color='magenta', style='solid', decorate='true', label='<step<font color="darkgreen"> stepdone stepfailed</font> &nbsp; tuneAtHome<font color="darkgreen"> tuneDone</font> &nbsp; getrobotstate<font color="darkgreen"> robotstate</font> &nbsp; getenvmap<font color="darkgreen"> envmap</font> &nbsp; setdirection<font color="darkgreen"> setdirectiondone</font> &nbsp; >',  fontcolor='magenta') >> robotmnemo
     planexec >> Edge(color='blue', style='solid',  decorate='true', label='<nomoremove &nbsp; nextmove &nbsp; >',  fontcolor='blue') >> planexec
     planexec >> Edge(color='blue', style='solid',  decorate='true', label='<move &nbsp; >',  fontcolor='blue') >> robotmnemo
     robotmnemo >> Edge(color='blue', style='solid',  decorate='true', label='<partnerstarted &nbsp; >',  fontcolor='blue') >> robotsmart
     robotsmart >> Edge(color='blue', style='solid',  decorate='true', label='<move &nbsp; setrobotstate &nbsp; >',  fontcolor='blue') >> robotmnemo
diag
