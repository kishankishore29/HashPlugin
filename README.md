# HashPlugin
An eclipse plugin to help a programmer override java's default implementation of hashCode().

# Plugin installation:
<b>Step 1:</b><br/>
1.Download both the zip files(HashPluginjars.zip  and HashPluginRelease.zip) from the repository.<br/>
2.Extract the HashPluginRelease folder from the HashPluginRelease.zip file <br/>
3.In the eclipse tool bar menu,navigate to the Help->Install New Software option.<br/>
4.Click on the add button which opens up a new dialog box.<br/>
5.Select the local option providing an optional name to your site.<br/>
6.Navigate to the HashPluginRelease folder.<br/>
7.Select Yes/OK for all the dialog boxes that appear next.<br/>
8.Eclipse will restart itself and you will be able to see a new menu named "HashPlugin" along with a new icon on the toolbar.<br/> 

<b>Step 2:</b><br/>
1.Unzip HashPluginsJars.zip in a folder.The unzipped folder contains 3 jar files inside.<br/>
2.Add these jar files to the build path of the project you want to use HashPlugin on.<br/>
3.Restart eclipse.<br/><br/>
<b>Both the steps are mandatory for HashPlugin to work properly in eclipse.</b> Following tutorial might prove useful in case any difficulties arise:<br>
http://www.vogella.com/tutorials/EclipsePlugin/article.html#install-feature-via-the-eclipse-update-manager

---
#Documentation
<b>Execution:</b> The class containing HashPlugin. It extends the AbstractHasndler class.<br><br>
<b>Methods defined in Execution</b>:<br><br>
 <b>static getStatistics():</b> A static method which displays the time taken by the currently chosen hash function.<br><br>
 <b>static summary():</b>A static method which displays the time taken by the currently chosen hash function as well as the                              previous times.<br><br> 
 <b>static close():</b> A static method to remove/clear all the previous times. The summary will contain only the current time                         after close() is called.<br><br> 
 
---
#Examples
 A java class using HashMap to add its own Objects as keys and Integers as values.<br>: 
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/1.png)<br>

After pressing the run button in the HashPlugin Menu, the user is prompted to choose the hash function to be used to override the default implementation of hashcode():<br>   
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/4.png)<br>
 
The next dialog box allows the user to choose the instance variables to be used while hashing(static variables are not included).<br>
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/5.png)<br>

Next,HashPlugin auto-generates the code for equals() and hashCode(). [ If eclipse does not auto refresh the file,press F5]<br>
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/2.png)<br>

After equals() and hashCode() have been generated, getStatistics(),summary() and close() can be called.
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/3.png)<br>

The output generated, when the above code is run has been shown below:<br>
![alt text](https://github.com/kishan2695/HashPlugin/blob/master/Screenshots/6.png)<br>


