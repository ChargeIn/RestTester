# Rest Tester Plugin

[![Current Version](https://img.shields.io/badge/version-1.5.2-green.svg)](https://github.com/ChargeIn/RestTester)

<!-- Plugin description -->
A simple open source rest api testing tool for Jetbrains
IDEAs ([Marketplace](https://plugins.jetbrains.com/plugin/20924-rest-tester))
<!-- Plugin description end -->

![Rest Tester Preview](https://github.com/ChargeIn/RestTester/blob/master/.github/demo.png)

## Usage

<li>Send and display http(s) request</li>
<li>Store defined request in a tree structure</li>
<li>Create different environments for different server.</li>
<li>Support for variables and different authentication types.</li>
<li>IntelliJ based code highlighting for the request and the result body.</li>
<li>Inline preview for web image resources</li>

## Environment Variables

![Variable Usage](https://github.com/ChargeIn/RestTester/blob/master/.github/variable-usage.png)

Set up variables which can be used across all request of the same environment, add a key-value pair to the table in the
"Variables" tab. A typical use-case is the base url of a rest server. To access a variables inside an input start typing
two double curly brackets followed by the variable name and two closing curly brackets (E.g. "{{ baseUrl }}" ).

![Variables Tab](https://github.com/ChargeIn/RestTester/blob/master/.github/variable-tab.png)

## Environments

To better support multiple server apis the plugin supports defining multiple environments, which
acts similar to projects or workspaces in Intellij. A new environment can be created by clicking the "Edit Environments"
button in the environment selector in the top left corner of the plugin. Environment do not share requests, variables or
authentication credentials. It is possible to choose a base url for each environment allowing to use relative request
urls and omitting the typical base url variable at the start of each request.

![Environment Selector](https://github.com/ChargeIn/RestTester/blob/master/.github/environment-selector.png)

## Testing

&#9989; Intellij Community 2024.2
&#9989; Intellij Ultimate 2024.2

---

## License

> You can check out the full license [here](https://github.com/ChargeIn/RestTester/blob/master/LICENSE)

