# BPMN-QT
A rule-based approach for an automatic quality assessment of a BPMN model. This application allows the user to validate certain criteria a given BPMN model, which was save in the BPMN-XML-Format. This can support a quality evaluation of the model.

Author: Andreas RAITH

## Requirements
* [JDK](https://openjdk.java.net/install/) for Java 8 

## Rules

Rules can be used to validate a certain criterion of a BPMN model. 

### Rule types

The user can use the following rules types to create new rules to validate:

| Rule type name | desciption |
| --- | --- |
| BpmnElementCountRule | Validate how often an element can exist inside a given model |
| BpmnGatewayMergeRule | Validate that only gateways are used to merge/split the sequence flow |
| BpmnPoolProcessRule | Validate that every expanded pool has a valid process |
| BpmnXmlValidationRule | Validate a BPMN model (XML) via an XML-Schema(*.xsd) |
| BpmnMetricRule | Validate the result of a certain metric |
| BpmnSoundnessRule | Validate the soundness of the sequence flow |
| BpmnFlowSequenceRule | Validate that a certain element has / doesn’t have a certain predecessor |
| BpmnComplexRule | Validate a set of rules |

### Rule import / export

Rules can be imported and exported from and to a qtrules-File. 

Warning: A import of a qtrules-File will override all currently defined rules!

## Metrics

Metrics can be used to determine certain characteristics of a given model. These metrics are not maintainable from the view of the user. All defined metrics are calculated during the validation of the model.

### Metric types

The following metric types can be used to create new metrics:

| Metric type name | description |
| --- | --- |
| ElementCountMetric  | Metric that counts how often certain elements are included in a given BPMN model |
| SequenceFlowSplitMetric  | Metric that tries to calculate the level of branching of the model |
| CognitiveWeightsMetric  | Metric that tries to evaluate how hard the model is to understand from a user perspective  |
