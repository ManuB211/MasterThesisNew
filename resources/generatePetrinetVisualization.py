import pm4py
import sys
from pm4py.objects.petri_net.importer import importer as pnml_importer
from pm4py.visualization.petri_net import visualizer as pn_visualizer
import os


def display_pnml(directory):
    
    for CPN in os.listdir(directory):
        file_path_in = directory+"/"+CPN
        file_path_out = directory+"/"+CPN.split(".")[0]+"_Visualization.png"
    
        if not os.path.exists(file_path_in):
            raise FileNotFoundError(f"The file {file_path_in} does not exist.")

        try:
            # Import the Petri net from the PNML file
            net, initial_marking, final_marking = pnml_importer.apply(file_path_in)

            # Visualize the Petri net
            gviz = pn_visualizer.apply(net, initial_marking, final_marking)

            # Save the visualization to a file
            pn_visualizer.save(gviz, file_path_out)

            print(f"Visualization saved to {file_path_out}. You can view it using an image viewer.")

        except Exception as e:
            print(f"An error occurred: {e}")

#Main
if __name__ == "__main__":
    timestamp = sys.argv[1];
    directory = "target/{}/CPNs_private".format(timestamp)
    display_pnml(directory)
