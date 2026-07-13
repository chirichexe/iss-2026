### conda install diagrams
import os
import shutil
import sys
from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.custom import Custom
from diagrams.generic.blank import Blank
from graphviz.backend.execute import ExecutableNotFound


BASE_DIR = Path(__file__).resolve().parent
QAKICONS_DIR = BASE_DIR / 'qakicons'
if not QAKICONS_DIR.exists():
    QAKICONS_DIR = BASE_DIR.parent / 'qakicons'
WINDOWS_GRAPHVIZ_BIN = Path('C:/Program Files/Graphviz/bin')

if WINDOWS_GRAPHVIZ_BIN.exists():
    os.environ['PATH'] += os.pathsep + str(WINDOWS_GRAPHVIZ_BIN)


def qak_node(label, icon_name):
    icon = QAKICONS_DIR / icon_name
    if icon.exists():
        return Custom(label, str(icon))
    return Blank(label)

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


def build_diagram():
    with Diagram('cargosystemArch', show=False, outformat='png', graph_attr=graphattr) as diag:
        with Cluster('env'):
            system = qak_node('', 'system.png')
            ### see https://renenyffenegger.ch/notes/tools/Graphviz/attributes/label/HTML-like/index
            with Cluster('ctxcargoservice', graph_attr=nodeattr):
                cargoservice = qak_node('cargoservice', 'symActorWithobjSmall.png')
            with Cluster('ctxcustomer', graph_attr=nodeattr):
                ledmock = qak_node('ledmock(ext)', 'externalQActor.png')
                ioportmock = qak_node('ioportmock(ext)', 'externalQActor.png')
            with Cluster('ctxdevices', graph_attr=nodeattr):
                hold = qak_node('hold(ext)', 'externalQActor.png')
                sonarmock = qak_node('sonarmock(ext)', 'externalQActor.png')
                markerdevice = qak_node('markerdevice(ext)', 'externalQActor.png')
            with Cluster('ctxrobot', graph_attr=nodeattr):
                cargorobotmock = qak_node('cargorobotmock(ext)', 'externalQActor.png')
            system >> Edge(label='sonardata', **evattr, decorate='true', fontcolor='darkgreen') >> cargoservice
            cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<robot_move<font color="darkgreen"> robot_done</font> &nbsp; >',  fontcolor='magenta') >> cargorobotmock
            cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<mark_container<font color="darkgreen"> marking_done</font> &nbsp; >',  fontcolor='magenta') >> markerdevice
            cargoservice >> Edge(color='magenta', style='solid', decorate='true', label='<get_slot<font color="darkgreen"> slot_reserved hold_full</font> &nbsp; >',  fontcolor='magenta') >> hold
            cargoservice >> Edge(color='blue', style='solid',  decorate='true', label='<led_ctrl &nbsp; >',  fontcolor='blue') >> ledmock
            cargoservice >> Edge(color='blue', style='solid',  decorate='true', label='<free_slot &nbsp; >',  fontcolor='blue') >> hold
    return diag


def main():
    if not QAKICONS_DIR.exists():
        print(f'Info: directory icone non trovata ({QAKICONS_DIR}); uso nodi testuali.')

    if shutil.which('dot') is None:
        print(
            "Errore: Graphviz non e' installato o l'eseguibile 'dot' non e' nel PATH.\n"
            "Installa Graphviz, poi rilancia: python3 Arch_cargosystem.py\n"
            "Arch Linux: sudo pacman -S graphviz\n"
            "Debian/Ubuntu: sudo apt install graphviz\n"
            "Windows: installa Graphviz e aggiungi la cartella bin al PATH"
        )
        return 1

    try:
        build_diagram()
    except ExecutableNotFound as exc:
        print(f"Errore: impossibile eseguire Graphviz 'dot': {exc}")
        return 1

    print('Diagramma generato: cargosystemarch.png')
    return 0


if __name__ == '__main__':
    sys.exit(main())
