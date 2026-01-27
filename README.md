# Dual Overhead Crane Scheduling with Priority-Based Rewards — Codebase and Data Generator

This repository contains the codebase and data/instance generator for the paper:

**“Dual Overhead Crane Scheduling with Priority-Based Rewards”**  
Deniz Nigar Yoltay and O. Erhun Kundakcioglu

The goal is to support **transparency and reproducibility** for experiments related to dual overhead crane scheduling under **priority-based rewards** (task prioritization and reward/penalty trade-offs).

---

## Repository contents (high level)

- **Core code (root-level)**
  - `src/` — Source code (algorithms, parameters, utilities).
  - `bin/` — Compiled Java class output (if/when compilation is used).
  - `src/run/Run.py` — Primary script to launch runs.

> Tip: If you later add a generator folder (e.g., `generator/`) and/or `instances/` and `results/`, you can extend this section with those paths.

---

## Installation & Running

### Prerequisites
- Python 3 (recommended: 3.9+)

### Run
From the repository root:

```bash
cd src/run
python3 Run.py
```

### Optional: clean old compiled Java classes
If you previously compiled Java code and you suspect stale `.class` files are causing issues, you can clean them first:

```bash
rm -f bin/*/*.class
```

> Note: This step is only needed if your workflow generates `.class` files and you run into unexpected behavior.

---

## Code structure (high level)

- `Launcher` — Application entry point (main method).
- `ProblemParameters` — User-defined problem/instance parameters.
- `AlgorithmParameters` — User-defined algorithm parameters.
- `Helpers` — Shared utility functions used across algorithms.

---

## Reproducibility notes

To reproduce results reliably, we recommend documenting (and version-controlling where applicable):

- Random seeds and instance-generation parameters
- Solver/algorithm parameters
- Environment info (OS, CPU/RAM) if runtime comparisons matter
- Any third-party solver requirements (and license constraints, if applicable)

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

---
