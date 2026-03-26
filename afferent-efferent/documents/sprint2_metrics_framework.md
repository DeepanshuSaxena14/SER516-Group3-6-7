# Sprint 2 Metrics Framework

# objective
in the sprint II it mainly targets on sofware process metrics in an agile framework. our team is accountable for making the metrics cruft and delivery on time as daily time set data that can be later displayed in grafana.

# Alloted Metrics
1. cruft
2. delivery on time

# Metric 1 - Cruft
cruft counts the rate of stories or tasks that are zero value or technical debt rated against the work tied explicitly to the value stream.

# Working Definition
- Value work - tasks or stories that explicitly contribute to user displayed service functionality, metric estimation, dashboard output, API behavior and other sprint deliverables,
- Non value work - setup, rework, learning tasks, technical debt cleanup,refactoring without direct  sprint value and other support tasks not explicitly tied to value delivery

# Formula
Cruft % = 
NonValueWork 
------------- x 100
TotalWork

# Daily evaluation
For every day in sprint -
1. get back all displayed sprint tasks or stories in Taiga
2. sort every item as value or non value
3. calculate the daily Cruft rate

# Metric 2 - delivery on time
delivery on time counts the rate of planned work items that are done by their projected date.

# Working definition
- On time - item reaches closed or done on or before projected completion date
- Late: item reaches Closed or Done after expected completion date
- If a task level due date is unavailable use sprint end date as the expected date

# Formula
DeliveryOnTime % =
OnTimeCompletedItems
--------------------- x 100
TotalPlannedItemsDue 

# daily evaluation
For every day in the sprint -
1. get sprint tasks or stories from taiga
2. predict expected completion date
3. Compare current status and closed date till expected date
4. calculate the daily percentage

# expected input data from taiga
- sprint or milestone data
- user stories
- tasks
- status
- created date
- modified date
- closed date if available
- due date if available
- story points
- business value
- tags or labels
- assigner information

# Proposed Output Schema
Every metric record should contain -
- date
- sprint_name
- metric_name
- metric_value

# additional detailed output may include -
- total_items
- value_items
- non_value_items
- on_time_items
- late_items

#  Proposed Service Flow
taiga API - fetch sprint data - normalize records - classify work - compute daily metrics - store results - expose for Grafana or API

#  storage Needs
The system should store:
- raw Taiga snapshots
- processed daily metric output
- logs

#  assumptions
- Work sorting for cruft will initially be rule based
- delivery on time may use sprint end date when task due date is absent
- Metrics should be computed daily over time as required for Sprint 2

#  risks or  Open questions
- taiga status history may require additional API calls
- Some tasks may be hard to classify as purely value or non value
- business value and story points may need team conventions in taiga