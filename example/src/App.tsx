import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import BackgroundService from 'upsell-background-actions';

export default function App() {
  const [result, setResult] = useState<number | undefined>();
  const sleep = (time) =>
    new Promise<void>((resolve) => setTimeout(() => resolve(), time));

  // You can do anything in your task such as network requests, timers and so on,
  // as long as it doesn't touch UI. Once your task completes (i.e. the promise is resolved),
  // React Native will go into "paused" mode (unless there are other tasks running,
  // or there is a foreground app).
  const veryIntensiveTask = async (taskDataArguments: any) => {
    // Example of an infinite loop task
    const { delay } = taskDataArguments;
    await new Promise(async (resolve) => {
      for (
        let i = 0;
        await BackgroundService.isBackgroundServiceRunning();
        i++
      ) {
        console.log(i);
        await sleep(delay);
      }
    });
  };

  const options = {
    taskName: 'Example',
    taskTitle: 'ExampleTask title',
    taskDesc: 'ExampleTask description',
    taskIcon: {
      name: 'ic_launcher',
      type: 'mipmap',
    },
    color: '#ff00ff',
    linkingURI: 'yourSchemeHere://chat/jane', // See Deep Linking for more info
    parameters: {
      delay: 1000,
    },
  };
  const restart1 = async () => {
    await BackgroundService.sendStopBroadcast();
    await BackgroundService.stopAlarm();
    await sleep(5000);
    console.log(
      'This is running services',
      await BackgroundService.listRunningServices()
    );
    await BackgroundService.setCallBack(restart1);
    await BackgroundService.start(veryIntensiveTask, options, 60000);
  };

  return (
    <View style={styles.container}>
      <Button
        title="Set Callback "
        onPress={async () => {
          await BackgroundService.setCallBack(restart1);
        }}
      />
      <Button
        title="stopAlarm"
        onPress={async () => {
          await BackgroundService.stopAlarm();
        }}
      />
      <Button
        title="Start Task "
        onPress={async () => {
          await BackgroundService.start(veryIntensiveTask, options, 60000);
        }}
      />
      <Button
        title="Stop Task "
        onPress={() => {
          BackgroundService.sendStopBroadcast();
        }}
      />
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
