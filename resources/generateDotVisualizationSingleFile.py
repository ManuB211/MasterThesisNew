import pm4py
import sys
import os
from graphviz import Source


def display_model(directory, filename):
    
    file_path_in = directory+"/"+filename
    file_path_out = directory+"/"+filename.split(".")[0]+"_Visualization"
    
    print(file_path_in)
    print(file_path_out)
    
    if not os.path.exists(file_path_in):
        raise FileNotFoundError(f"The file {file_path_in} does not exist.")

    try:
        # Import the model from the DOT file
        with open(file_path_in, "r") as file:
            model_content = file.read()
            
        # Create Graph object
        model_graph = Source(model_content)

        # Save the model as image
        model_graph.render(file_path_out, format="png", cleanup=True)
        print(f"Visualization saved to {file_path_out}")

    except Exception as e:
        raise Error(f"An error occurred: {e}")

    
# Main
if __name__ == "__main__":
    timestamp = sys.argv[1]
    filename = sys.argv[2]
    
    directory = "target/{}".format(timestamp)
    
    display_model(directory, filename)
