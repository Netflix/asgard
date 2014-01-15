## Ways to Contribute

There are many ways to contribute to Asgard:

* Spreading the word by talking, tweeting, blogging, presenting, submitting talks, writing tutorials or articles (or a book!), sharing success stories, adding your project/company to [Who is Using Asgard](some_link), etc.
* Helping other users by participating in [Asgard Users Google Group](https://groups.google.com/forum/#!forum/AsgardUsers)
* Improving and extending our [wiki](https://github.com/Netflix/asgard/wiki)
* Fixing open issues listed in the [issue tracker](https://github.com/Netflix/asgard/issues)
* Proposing, discussing, and implementing new features
* [Join us at Netflix](http://jobs.netflix.com/) and work on Asgard fulltime! 
* Suprising us with some other form of contribution!

All forms of contribution are very much appreciated.

## Communication

Good communication makes a big difference. We are always eager to listen, reflect, and discuss. Don't hesitate to get in touch via the [issue tracker](https://github.com/Netflix/asgard/issues) or our [user forum](https://groups.google.com/forum/#!forum/AsgardUsers). Choose whatever medium feels most appropriate.

## Contributing to Asgard

To contribute code or documentation, please submit a pull request to the [GitHub repository](https://github.com/Netflix/asgard). 

A good way to familiarize yourself with the codebase and contribution process is to look for and tackle low-hanging fruits in the [issue tracker](https://github.com/Netflix/asgard/issues). Before embarking on a more ambitious contribution, please quickly [get in touch](#communication) with us. This will help to make sure that the contribution is aligned with Asgard's overall direction and goals, and gives us a chance to guide design and implementation where needed. 

**We appreciate your effort, and want to avoid a situation where a contribution requires extensive rework (by you or by us), sits in the queue for a long time, or cannot be accepted at all!**

### Proposing new features

If you would like to implement a new feature, please [raise an issue](https://github.com/Netflix/asgard/issues) before sending a pull request so the feature can be discussed.
This is to avoid you spending your valuable time working on a feature that the project developers are not willing to accept into the code base.

### Fixing bugs

If you would like to fix a bug, please [raise an issue](https://github.com/Netflix/asgard/issues) before sending a pull request so it can be discussed.
If the fix is trivial or non controversial then this is not usually necessary.

## Coding style guidelines

The following are some general guide lines to observe when contributing code:

1. Everything needs to be tested
1. All source files must have the appropriate ASLv2 license header
1. All source files use an indent of 2 spaces
1. Everything needs to be tested (see point #1)

The build processes checks that most of the above conditions have been met.


### What Makes a Good Pull Request

When reviewing pull requests, we value the following qualities (in no particular order):

* Carefully crafted, clean and concise code
* Small, focused commits that tackle one thing at a time
* New code blends in well with existing code, respecting established coding standards and practices
* Tests are updated/added along with the code, communicate intent and cover important cases (see [Tests](#tests) for additional information)
* Documentation (Javadoc, Groovydoc, reference documentation) is updated/added along with the code

Don't be intimidated by these words. Pull requests that satisfy Asgard's overall direction and goals (see above), are crafted carefully, and aren't aiming too high, have a good chance of getting accepted. Before doing so, we may ask for some concrete improvements to be made, in which case we hope for your cooperation.

### Compatibility

Asgard supports JRE 1.6 and higher. Therefore, language features and APIs that are only available in Java 1.7 or higher cannot be used. Exceptions to this rule need to be discussed beforehand. The same goes for changes to user-visible behavior.

### Tests

All tests are written in Spock.

The Asgard Team
