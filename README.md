# Dual Overhead Crane Scheduling with Priority-Based Rewards — Codebase and Data Generator

This repository contains the source code and the instance generator for the paper:

**“Dual Overhead Crane Scheduling with Priority-Based Rewards”**  
Deniz Nigar Yoltay and O. Erhun Kundakcioglu

The paper studies the Dual Overhead Crane Scheduling Problem with task priorities (DOCSP), where two non-crossing cranes on a shared runway are assigned and sequenced to maximize the total priority of tasks completed within the planning horizon. The repository provides (i) the heuristics, (ii) the dynamic programming variants (DP, RDP, and ADP), (iii) the time-space network (TSN) formulations, and (iv) the test instances used in the computational study.

---

## Repository contents

| Path | Description |
|---|---|
| `src/data/` | Instance generator (`Problem`, `Job`, `Crane`), algorithm dispatch (`ProblemSetting`), and CSV output (`Printing`). |
| `src/heuristics/` | Dispatching heuristics Hr, Hr/t, and Hr/t* (`GreedySolution`), and the Hr/t* variant projected onto the TSN for the warm start (`HeuristicsNetworkSol`). |
| `src/DP/` | Exact DP and RDP (`DynamicProgramming`), ADP with the value-function approximation MILP (`DPLP`, `GurobiSolution`), and shared state/conflict logic (`Snapshot`, `CraneStatus`, `Action`, `Helpers`). |
| `src/tsnSolution/` | TSN construction (`Network`, `Node`), variables, objective, conflict constraints (`TsConstraints`), the base/extended models (`TsGurobiSol`), and the extended model with warm start (`TsBounding`). |
| `src/parameters/` | Problem parameters (`ProblemParameters`, including the solver time limit) and algorithm enumerations (`AlgorithmParameters`). |
| `src/run/` | Command-line entry point (`Launcher`), a local batch launcher (`LauncherLocal`), and the batch driver script (`Run.py`). |
| `data/` | The 100 test instances used in the paper (see [Test instances](#test-instances)). |

---

## Requirements

- **Java Development Kit (JDK).**
- **Gurobi 12.0** with a valid license. Copy `gurobi.jar` from your Gurobi installation into `src/`, and make sure the Gurobi native library can be found (e.g., `GUROBI_HOME` and `DYLD_LIBRARY_PATH` on Mac, or `LD_LIBRARY_PATH` on Linux). Gurobi is needed for ADP and all TSN models. The heuristics, DP, and RDP do not call the solver, but the code still has to compile against `gurobi.jar`.
- **Gson 2.10.1** and **Guava 31.1** (`gson-2.10.1.jar` and `guava-31.1-jre.jar`), copied into `src/`. These jars are not included in the repository.
- **Python 3 with pandas** (only for `Run.py`).

The experiments in the paper were run on an Intel Core i9-13900K CPU (3.0 GHz) with 64 GB of RAM, with a 60-minute time limit unless stated otherwise.

---

## Running

### Batch runs with `Run.py`

```bash
cd src/run
python3 Run.py
```

`Run.py` compiles the code into `bin/` at the repository root, switches to `bin/`, and calls `Launcher` once per configuration. A configuration is skipped if its result already appears in `bin/results_afterinforms_oneHour.csv`. Note that only the TSN base model block (algorithm 11 for 30, 40, and 50 tasks) is active in the current script. The blocks for the heuristics, RDP, ADP, and DP are kept in the file as commented-out code, and they can be re-enabled to run those experiments.

### Single runs with `Launcher`

From the repository root:

```bash
javac -cp "src/gson-2.10.1.jar:src/guava-31.1-jre.jar:src/gurobi.jar:src" -d bin src/run/Launcher.java
```

```bash
cd bin && java -cp "../src/gson-2.10.1.jar:../src/guava-31.1-jre.jar:../src/gurobi.jar:." run.Launcher 0 10 25 20 s 0 11 1
```

`Launcher` takes eight positional arguments:

| Position | Meaning | Values used in the paper |
|---|---|---|
| 1 | Detailed print flag | `0` |
| 2 | Random seed | `2 4 5 10 13 15 20 55 65 87` |
| 3 | Number of columns (track length *K*) | `25` (Table 14 also uses 10, 15, 20, 35, and 50) |
| 4 | Number of tasks *T* | `5 10 12 14 16 20 25 30 40 50` |
| 5 | Task duration class | `s` (short, durations 1–3) |
| 6 | RDP restriction ratio α (algorithms 4 and 5; `0` otherwise) | `0.2 0.4 0.6` |
| 7 | Algorithm ID (see below) | |
| 8 | TSN discretization interval *I* (TSN models; `0` otherwise) | `1`–`10` |

### Algorithm IDs

| ID | Label in `Launcher` | Paper label |
|---|---|---|
| 1 | Greedy (reward) | Hr (Section 4.1) |
| 2 | Greedy (reward per time) | Hr/t (Section 4.1) |
| 3 | Greedy (reward per time + conflict) | Hr/t* (Section 4.1) |
| 4 | Branching (reward) | RDPr (Section 4.3) |
| 5 | Branching (reward per time + conflict) | RDPr/t* (Section 4.3) |
| 6 | ADP | ADP (Section 4.4) |
| 7 | Exact | DP (Section 4.2) |
| 8 | ADP_RDP | Not reported in the paper |
| 9 | TS Extended | TSN extended model with push arcs (Section 5.7) |
| 10 | TS Extended Bounding | TSN extended model with the Hr/t* warm start (Section 6.5.2) |
| 11 | TS Base | TSN conflict-aware base model (Section 5.6) |

Note that `LauncherLocal` uses a different numbering for some of these algorithms (e.g., 10 for ADP and 16 for TS Base). We recommend `Launcher` and the IDs above.

### Output

Each run appends one line to `results_afterinforms_oneHour.csv` in the working directory, containing the number of tasks, the number of columns, the algorithm label, the seed, the total reward, and the elapsed time. TSN runs also report the discretization interval and the final MIP gap. Gurobi logs are written to `tsn<T>.log`.

---

## Test instances

`data/` contains one job file and one crane file for each of the 10 seeds and 10 task counts listed above (100 instances):

- `job_s_<seed>_<T>_d_s_c_25.json` lists, for each task, the origin and destination columns (`fromColumn`, `toColumn`), the processing times at the origin and destination (`duration`, `duration2`), and the priority (`prio`). Note that `priority` holds the same value with a small tie-breaking offset.
- `crane_s_<seed>_<T>_d_s_col_25.json` gives the initial positions of the two cranes (column 0 and column *K*).

Instances are generated in the `Problem` constructor with `java.util.Random(seed)`. Origins and destinations are uniform over the columns, processing times are uniform integers in {1, 2, 3}, and priorities are uniform integers in {1, …, 4}. The planning horizon is set to half of the total travel and processing time of all tasks, as described in Section 6.1.

**Regenerating instances.** `Launcher` regenerates each instance from its seed on every run and writes the two JSON files to the working directory, overwriting any existing files with the same name. Running `Launcher` with the arguments above reproduces the files in `data/` exactly. However, `LauncherLocal` produces *different* instances from the same seeds. The reason is the string comparison `duration == "s"` in `Problem`, which is true for a string literal in `LauncherLocal` but false for a command-line argument, so the random number stream is shifted by one draw. Note also that loading instances from JSON (`loadFromJson`) is disabled in `Launcher`, and the file names in `Problem.loadFromJson` are hard-coded.

---

## Reproducing the tables in the paper

The table below shows how each computational result maps to the code, and what is needed beyond the current repository.

| Table | Algorithms (IDs) | Settings | Notes |
|---|---|---|---|
| 5 — Heuristics | 1, 2, 3 | *T* = 12, 30, 50; 10 seeds | Supported. Re-enable the heuristics block in `Run.py`. |
| 6 — DP waiting variants | 7 | *T* = 4, 5, 10, 12; 10 seeds | **Partially supported.** The code implements multi-step idle waiting only (the idle successor state is evaluated at every stage). The single-step variant is not selectable and requires a code change. The 4-task instances are not in `data/` but can be generated with the same seeds. |
| 7 — RDP | 4, 5 | *T* = 14, 16, 20; α = 0.2, 0.4, 0.6 | Supported. α is passed as argument 6. |
| 8 — Base vs. extended TSN | 11, 9 | Five instances per size; *I* as in the table | Supported. The two-hour limit requires setting `timeLimit = 7200` in `ProblemParameters`. |
| 9 — Warm start | 9, 10 | *I* as in the table | Supported. |
| 10 — Discretization | 11 | *I* as in the table | **Partially supported.** Rewards, solve times, and MIP gaps are reported. However, model build time is not reported separately (the elapsed time includes model construction), so the build time column cannot be reproduced directly. |
| 11, 12 — Best algorithms | 3, 5, 6, 11 | α as in Section 6.4; *I* from Table 11 | Supported. |
| 13 — Priority distributions | 3, 11 | *T* = 15, *K* = 25, *I* = 1; five seeds | **Not supported.** The generator only draws uniform priorities in {1, …, 4} (scenario U4). Scenarios EQ, U8, DS2, and DS3 require changes to `Job`. |
| 14 — Track length | 3, 11 | *T* = 15, *I* = 1; five seeds; *K* = 10, 15, 20, 25, 35, 50 | Supported through argument 3. The 15-task instances are not in `data/` and are generated at run time. |
| 15 — Planning horizon | 3, 11 | *T* = 15, *K* = 25, *I* = 1; five seeds; *h* = 0.33, 0.5, 0.67, 1.0 | **Not supported.** The horizon is fixed at *h* = 0.5 in `Problem`. Other values require a code change. |

Note that the seeds used for the 15-task instances in Section 6.7 are not recorded in the repository. In addition, the conflict-blind model of Section 5.5 is available in the code (`tsCaseSet.ConflictFree`) but is not exposed through `Launcher`, since it is not part of the reported experiments.

---

## How to cite

If you use or reference this repository, please cite the paper and (optionally) the repository.

**Plain text**
> Yoltay, D. N. & Kundakcioglu, O. E., *“Dual Overhead Crane Scheduling with Priority-Based Rewards”*. GitHub repository: https://github.com/OEKundakcioglu/CraneSchedulingWithTaskPriorities

**BibTeX (templates)**
```bibtex
@misc{YoltayKundakcioglu_DualCraneRewards,
  author       = {Yoltay, Deniz Nigar and Kundakcioglu, O. Erhun},
  title        = {Dual Overhead Crane Scheduling with Priority-Based Rewards},
  note         = {Manuscript},
  year         = {2025}
}

@misc{YoltayKundakcioglu_CraneSchedulingRepo,
  author       = {Yoltay, Deniz Nigar and Kundakcioglu, O. Erhun},
  title        = {Dual Overhead Crane Scheduling with Priority-Based Rewards: Codebase and Data Generator},
  url          = {https://github.com/OEKundakcioglu/CraneSchedulingWithTaskPriorities},
  note         = {Accessed: YYYY-MM-DD},
  year         = {2025}
}
```

---

## License

This repository is released under the **MIT License**.

```
MIT License

Copyright (c) 2025 Erhun Kundakcıoğlu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
