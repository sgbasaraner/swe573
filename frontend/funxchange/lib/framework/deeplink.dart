import 'package:flutter/material.dart';
import 'package:funxchange/framework/cache.dart';
import 'package:funxchange/framework/di.dart';
import 'package:funxchange/screens/event.dart';
import 'package:funxchange/screens/profile.dart';
import 'package:funxchange/screens/request_list.dart';

class DeeplinkNavigator {
  final BuildContext context;

  DeeplinkNavigator(this.context);

  static DeeplinkNavigator of(BuildContext context) =>
      DeeplinkNavigator(context);

  void navigate(String deeplink) async {
    final parsedDeeplink = _parse(deeplink);
    switch (parsedDeeplink.key) {
      case _DeeplinkType.User:
        Navigator.of(context).push(MaterialPageRoute(
          builder: (ctx) => ProfilePage(userId: parsedDeeplink.value!),
        ));
        break;
      case _DeeplinkType.Event:
        final imgMap = await Cache.interestImageMap();
        final event = await DIContainer.activeSingleton.eventRepo
            .fetchEvent(parsedDeeplink.value!);
        Navigator.of(context).push(MaterialPageRoute(builder: (ctx) {
          return EventPage(event: event, image: imgMap[event.category]!);
        }));
        break;
      case _DeeplinkType.Requests:
        Navigator.of(context).push(MaterialPageRoute(builder: (ctx) {
          return Scaffold(
            appBar: AppBar(
              title: const Text("Pending Requests"),
            ),
            body: const RequestList(),
          );
        }));
    }
  }

  static const _deeplinkPrefix = "funxchange://";

  MapEntry<_DeeplinkType, String?> _parse(String deeplink) {
    if (!deeplink.startsWith(_deeplinkPrefix)) {
      throw ArgumentError.value(deeplink);
    }

    final trimmed = deeplink.replaceFirst(_deeplinkPrefix, "");
    final splitted = trimmed.split("/");

    final target = splitted[0];
    switch (target) {
      case "users":
        return MapEntry(_DeeplinkType.User, splitted[1]);
      case "events":
        return MapEntry(_DeeplinkType.Event, splitted[1]);
      case "requests":
        return const MapEntry(_DeeplinkType.Requests, null);
      default:
        throw ArgumentError.value(deeplink);
    }
  }
}

enum _DeeplinkType {
  User,
  Event,
  Requests,
}
