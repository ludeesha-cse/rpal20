import os
import subprocess

def execute_rpal(filename):
    x=filename.split(".")[2]
    if (x!="rpal"):
        return "Not rpal program"
    try:
        # Replace 'rpal' with the actual command you want to execute
        command = f'java App {filename}'
        result = subprocess.check_output(command, shell=True, text=True)
        return result.strip()
    except subprocess.CalledProcessError as e:
        return f'Error executing command: {e}\n'

def main():
    input_directory = '.'  # Replace '.' with the path to your directory if needed

    # Get a list of all files in the directory
    files = [f for f in os.listdir(input_directory) if os.path.isfile(os.path.join(input_directory, f))]

    with open('result.txt', 'w') as output_file:
        for filename in files:
            try:
                x=filename.split(".")[1]
                print(x)
            except:
                continue
            if (x!="rpal"):
                continue
            result = execute_rpal(os.path.join(input_directory, filename))
            output_file.write(f'{filename}-->{result}\n')

if __name__ == "__main__":
    main()
