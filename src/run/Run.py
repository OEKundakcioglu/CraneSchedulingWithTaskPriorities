import os
import subprocess
import pandas as pd

comd='java' 


numberOfJobs = ['5','10', '12', '14', '16', '20', '25', '30', '40', '50']
numberOfJobsExact = ['10']
numberOfColumns = ['25']
seedNo = ['2', '4', '5', '10', '13', '15', '20', '87', '65', '55']
duration = ["s"]
stageCoeff = ['0.2', '0.4', '0.6', '0.8']
problemHeuristics = ['1', '2', '3']
problemExact = ['7']
problemTS = ['11']#['9', '10', '11']
problemStage = ['4', '5']
problemADP = ['6', '8']


problem_descriptions = {
'1': 'Greedy (reward)',
'2': 'Greedy (reward per time)',
'3': 'Greedy (reward per time + conflict)',
'4': 'Branching{}% (reward)',
'5': 'Branching{}% (reward per time + conflict)',
'6': 'ADP',
'7': 'Exact',
'8': 'ADP_RDP',
'9': 'TS Extended',
'10': 'TS Extended Bounding',
'11': 'TS Base'
}

def read_results(file_path):
    try:
        with open(file_path, 'r') as f:
            data = [line.strip().split(',') for line in f]
        columns = ['Job', 'Job_Number', 'Column', 'Column_Number', 'Problem Type', 'seed', 'seed_Number', 'reward_Label', 'reward', 'interval_Label', 'interval', 'elapsed time create_Label', 'elapsed time create', 'elapsed time solve_Label', 'elapsed time solve']
        df = pd.DataFrame(data, columns=columns)
        return df
    except FileNotFoundError:
        return None

def argument_exists(df, job, seed, col, pro, interval=None, coeff=None):
    if coeff is not None and pro in problemStage:
        problem_type = problem_descriptions[pro].format(int(float(coeff) * 100))
    else:
        problem_type = problem_descriptions.get(pro, pro)
    if interval is not None:
        filtered_df = df[(df['Job_Number'] == str(job)) &
                         (df['Column_Number'] == str(col)) &
                         (df['Problem Type'] == problem_type) &
                         (df['seed_Number'] == str(seed)) &
                         (df['interval'] == str(interval))]

        if filtered_df.empty:
            return False

        if job in ['30', '40', '50'] and filtered_df.iloc[0]['reward'] == '0.0':
            return True #False    
        else:
            return not df[(df['Job_Number'] == str(job)) & (df['Column_Number'] == str(col)) & (df['Problem Type'] == problem_type) & (df['seed_Number'] == str(seed)) & (df['interval'] == str(interval))].empty
    elif coeff is not None:
        return not df[(df['Job_Number'] == str(job)) & (df['Column_Number'] == str(col)) & (df['Problem Type'] == problem_type) & (df['seed_Number'] == str(seed)) & (df['reward'] == str(coeff))].empty
    else:
        return not df[(df['Job_Number'] == str(job)) & (df['Column_Number'] == str(col)) & (df['Problem Type'] == problem_type) & (df['seed_Number'] == str(seed))].empty


def compile_java():  
    src_dir = "../../src"
    bin_dir = "../../bin"
    module_path = f"{src_dir}/gson-2.10.1.jar:{src_dir}/guava-31.1-jre.jar:{src_dir}/gurobi.jar:{src_dir}"

    os.makedirs(bin_dir, exist_ok=True)

    compile_command = [
        "javac",
        "-cp", module_path,
        "-d", bin_dir,
        f"{src_dir}/run/Launcher.java"
    ]

    result = subprocess.run(compile_command, capture_output=True, text=True)
    
    if result.returncode != 0:
        print("Compilation failed:")
        print(result.stderr)
    else:
        print("Compilation succeeded")

def run_java(args):
    src_dir = "../src"
    module_path = f"{src_dir}/gson-2.10.1.jar:{src_dir}/guava-31.1-jre.jar:{src_dir}/gurobi.jar:."

    run_command = [
        "java",
        "-cp", module_path,
        "run/Launcher"
    ] + args.split()
    #print(os.getcwd())
    #for i in run_command:
    #    print(i)

    result = subprocess.run(run_command, capture_output=True, text=True)
    
    if result.returncode != 0:
        print("Execution failed:")
        print(result.stderr)

def run_command(a):  
    run_java(a)

compile_java()

#switch directory
os.chdir('../../bin')

results_file = './results_afterinforms_oneHour.csv'

# Read the existing results
results_df = read_results(results_file)
results_exist = results_df is not None


for job in numberOfJobs:
    for seed in seedNo:
        for col in numberOfColumns:
            for dur in duration:
                for pro in problemTS:
                    """if job in ['5', '10', '12', '14', '16']:
                        a = f"0 {seed} {col} {job} {dur} 0 {pro} 1"
                        if not results_exist or not argument_exists(results_df, job, seed, col, pro, interval='1'):
                            run_command(a)
                    elif job in ['20', '25']:
                        timeInterval = ['1', '2', '3']
                        for time in timeInterval:
                            a = f"0 {seed} {col} {job} {dur} 0 {pro} {time}"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pro, interval=time):
                                run_command(a)"""
                    if job == '30':
                        timeInterval = ['1']#['2', '3', '4', '5']
                        for time in timeInterval:
                            a = f"0 {seed} {col} {job} {dur} 0 {pro} {time}"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pro, interval=time):
                                run_command(a) 
                    elif job == '40':
                        timeInterval = ['2']#['3', '4', '5', '6']
                        for time in timeInterval:
                            a = f"0 {seed} {col} {job} {dur} 0 {pro} {time}"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pro, interval=time):
                                run_command(a)
                    elif job == '50':
                        timeInterval = ['4']#['5', '6','7','8', '10']
                        for time in timeInterval:
                            a = f"0 {seed} {col} {job} {dur} 0 {pro} {time}"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pro, interval=time):
                                run_command(a)
    print("Time Space Network number of", job, " job finished")
print("Time Space Network Tests finished")

"""# Second part
for job in numberOfJobs:
    for seed in seedNo:
        for col in numberOfColumns:
            for dur in duration:
                for pro in problemHeuristics:
                    a = f"0 {seed} {col} {job} {dur} 0 {pro} 0"
                    if not results_exist or not argument_exists(results_df, job, seed, col, pro):
                        run_command(a)  
                for pro_adp in problemADP:
                    a = f"0 {seed} {col} {job} {dur} 0 {pro_adp} 0"
                    if not results_exist or not argument_exists(results_df, job, seed, col, pro_adp):
                        run_command(a)            
                for pros in problemStage:
                    if job in ['10', '12']:
                        stageCoeff = ['0.4', '0.6']
                        for coeff in stageCoeff:
                            b = f"0 {seed} {col} {job} {dur} {coeff} {pros} 0"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pros, coeff=coeff):
                                run_command(b)
                    if job in ['5']:
                        stageCoeff = ['0.8']
                        for coeff in stageCoeff:
                            b = f"0 {seed} {col} {job} {dur} {coeff} {pros} 0"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pros, coeff=coeff):
                                run_command(b)
                    if job == '14':
                        stageCoeff = ['0.2', '0.4']
                        for coeff in stageCoeff:
                            b = f"0 {seed} {col} {job} {dur} {coeff} {pros} 0"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pros, coeff=coeff):
                                run_command(b)
                    elif job == '16':
                        stageCoeff = ['0.2']
                        for coeff in stageCoeff:
                            b = f"0 {seed} {col} {job} {dur} {coeff} {pros} 0"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pros, coeff=coeff):
                                run_command(b)
                    elif job == '20':
                        stageCoeff = ['0.1']
                        for coeff in stageCoeff:
                            b = f"0 {seed} {col} {job} {dur} {coeff} {pros} 0"
                            if not results_exist or not argument_exists(results_df, job, seed, col, pros, coeff=coeff):
                                run_command(b)
                    
    print("Heuristics and RDP number of", job, " job finished")
print("Heuristics and RDP tests finished")

# Third part
for job in numberOfJobsExact:
    for seed in seedNo:
        for col in numberOfColumns:
            for dur in duration:
                for pro in problemExact:
                    a = f"0 {seed} {col} {job} {dur} 0 {pro} 0"
                    if not results_exist or not argument_exists(results_df, job, seed, col, pro):
                        run_command(a)
    print("DP number of", job, " job finished")
print("DP tests finished")"""


