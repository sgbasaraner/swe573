import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:funxchange/framework/colors.dart';
import 'package:funxchange/framework/di.dart';
import 'package:funxchange/screens/event_list.dart';
import 'package:funxchange/screens/new_event.dart';

class FeedPage extends StatelessWidget {
  const FeedPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            Navigator.of(context).push(
              MaterialPageRoute(builder: (ctx) => const NewEventScreen()),
            );
          },
          child: const Icon(Icons.add),
        ),
        appBar: AppBar(
          bottom: const TabBar(
            indicatorColor: FunColor.sunray,
            tabs: [
              Tab(
                text: "Everyone",
              ),
              Tab(
                text: "Followed",
              ),
            ],
          ),
          title: const Text('Feed'),
        ),
        body: TabBarView(
          children: [
            EventList(
              eventFetcher: (limit, offset) => DIContainer
                  .activeSingleton.eventRepo
                  .fetchFeed(limit, offset, false),
            ),
            EventList(
              eventFetcher: (limit, offset) => DIContainer
                  .activeSingleton.eventRepo
                  .fetchFeed(limit, offset, true),
            ),
          ],
        ),
      ),
    );
  }
}
